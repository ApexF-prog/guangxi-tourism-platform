package com.gxtourism.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gxtourism.common.BusinessException;
import com.gxtourism.common.VipPolicy;
import com.gxtourism.dto.OrderCreateDTO;
import com.gxtourism.entity.OrderInfo;
import com.gxtourism.entity.Product;
import com.gxtourism.entity.ScenicSpot;
import com.gxtourism.entity.Shop;
import com.gxtourism.entity.User;
import com.gxtourism.mapper.OrderInfoMapper;
import com.gxtourism.mapper.ProductMapper;
import com.gxtourism.mapper.ScenicSpotMapper;
import com.gxtourism.mapper.ShopMapper;
import com.gxtourism.mapper.UserMapper;
import com.gxtourism.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderInfoMapper orderMapper;
    private final ProductMapper productMapper;
    private final ScenicSpotMapper spotMapper;
    private final ShopMapper shopMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public OrderInfo create(Long userId, OrderCreateDTO dto) {
        if (dto.getProductId() == null || dto.getQuantity() == null
                || dto.getQuantity() < 1 || dto.getQuantity() > 9) {
            throw new BusinessException("下单参数不正确");
        }
        Product product = productMapper.selectById(dto.getProductId());
        if (product == null || product.getStatus() != 1) throw new BusinessException("该商品已下架");

        OrderInfo order = new OrderInfo();
        order.setOrderNo("GX" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(100, 1000));
        order.setUserId(userId);
        order.setProductId(product.getId());
        order.setProductName(product.getName());
        order.setProductType(product.getType());
        order.setShopId(product.getShopId());
        // 商家归属一律以「商品所属店铺」为准，保证游客下单后订单出现在正确的商家后台
        Long merchantId = product.getMerchantId();
        if (product.getShopId() != null) {
            Shop shop = shopMapper.selectById(product.getShopId());
            if (shop != null) {
                merchantId = shop.getMerchantId();
                if (product.getSpotId() == null) product.setSpotId(shop.getSpotId());
            }
        }
        order.setMerchantId(merchantId);
        order.setSpotId(product.getSpotId());
        if (product.getSpotId() != null) {
            ScenicSpot spot = spotMapper.selectById(product.getSpotId());
            order.setSpotName(spot == null ? null : spot.getName());
        }
        order.setQuantity(dto.getQuantity());
        // 金额一律以后端库价重算，不信任前端传参；会员（VIP 有效期内）全站 9 折
        User buyer = userMapper.selectById(userId);
        BigDecimal unit = VipPolicy.unitPrice(buyer, product.getPrice());
        order.setUnitPrice(unit);
        order.setTotal(unit.multiply(BigDecimal.valueOf(dto.getQuantity())));
        order.setVipPrice(VipPolicy.valid(buyer) ? 1 : 0);

        // 门票/套餐必须选未来的游玩日期
        if (Product.TICKET.equals(product.getType()) || Product.PACKAGE.equals(product.getType())) {
            if (dto.getVisitDate() == null || dto.getVisitDate().isBefore(LocalDate.now())) {
                throw new BusinessException("请选择今天或之后的游玩日期");
            }
            order.setVisitDate(dto.getVisitDate());
        }

        // 并发安全扣库存：stock >= quantity 才允许更新，影响行数为 0 即库存不足
        LambdaUpdateWrapper<Product> uw = new LambdaUpdateWrapper<>();
        uw.eq(Product::getId, product.getId())
          .setSql("stock = stock - " + dto.getQuantity())
          .ge(Product::getStock, dto.getQuantity());
        if (productMapper.update(null, uw) == 0) throw new BusinessException("库存不足，请减少数量");

        order.setStatus(OrderInfo.PENDING);
        order.setPayType("ALIPAY");
        // 美食/饮品生成取餐号：店铺字母前缀 + 4 位数字（如 A-3271）
        if (Product.FOOD.equals(product.getType()) || Product.DRINK.equals(product.getType())) {
            String letter = product.getShopId() == null ? "X"
                    : String.valueOf((char) ('A' + (product.getShopId() - 1) % 26));
            order.setPickNo(letter + "-" + ThreadLocalRandom.current().nextInt(1000, 10000));
        }
        orderMapper.insert(order);
        return order;
    }

    @Override
    public IPage<OrderInfo> myOrders(Long userId, long page, long size) {
        return orderMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<OrderInfo>()
                        .eq(OrderInfo::getUserId, userId)
                        .orderByDesc(OrderInfo::getCreatedAt));
    }

    @Override
    public OrderInfo getByOrderNo(Long userId, String orderNo) {
        OrderInfo order = orderMapper.selectOne(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getOrderNo, orderNo));
        if (order == null || !order.getUserId().equals(userId)) throw new BusinessException("订单不存在");
        return order;
    }

    @Override
    @Transactional
    public void cancel(Long userId, String orderNo) {
        OrderInfo order = getByOrderNo(userId, orderNo);
        if (!OrderInfo.PENDING.equals(order.getStatus())) throw new BusinessException("当前状态不可取消");
        order.setStatus(OrderInfo.CANCELED);
        orderMapper.updateById(order);
        // 回补库存
        LambdaUpdateWrapper<Product> uw = new LambdaUpdateWrapper<>();
        uw.eq(Product::getId, order.getProductId()).setSql("stock = stock + " + order.getQuantity());
        productMapper.update(null, uw);
    }

    /** 商家端使用：校验店铺归属 */
    public Shop ownedShop(Long merchantId, Long shopId) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null || !shop.getMerchantId().equals(merchantId)) {
            throw new BusinessException("店铺不存在或无权操作");
        }
        return shop;
    }
}
