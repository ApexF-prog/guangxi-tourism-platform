package com.gxtourism.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gxtourism.common.BusinessException;
import com.gxtourism.common.Result;
import com.gxtourism.entity.*;
import com.gxtourism.mapper.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 商家后台：数据完全隔离，只能看到/操作自己的店铺与商品 */
@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private final UserMapper userMapper;
    private final ShopMapper shopMapper;
    private final ProductMapper productMapper;
    private final OrderInfoMapper orderMapper;

    // ================= 店铺 =================

    /** 我的店铺列表 */
    @GetMapping("/shops")
    public Result<List<Shop>> myShops(HttpServletRequest req) {
        return Result.ok(shopMapper.selectList(new LambdaQueryWrapper<Shop>()
                .eq(Shop::getMerchantId, uid(req))
                .orderByAsc(Shop::getId)));
    }

    /** 开设新店铺 */
    @PostMapping("/shop")
    public Result<Shop> createShop(@RequestBody Shop shop, HttpServletRequest req) {
        requireMerchant(req);
        if (shop.getName() == null || shop.getName().isBlank()) throw new BusinessException("店铺名不能为空");
        if (!Shop.FOOD.equals(shop.getType()) && !Shop.DRINK.equals(shop.getType())) {
            throw new BusinessException("店铺类型必须是 FOOD 或 DRINK");
        }
        if (shop.getSpotId() == null) throw new BusinessException("请选择挂靠景区");
        shop.setMerchantId(uid(req));
        if (shop.getStatus() == null) shop.setStatus(1);
        if (shop.getRating() == null) shop.setRating(new BigDecimal("4.5"));
        shopMapper.insert(shop);
        return Result.ok(shop);
    }

    // ================= 商品（增删改查 + 同名查重） =================

    /** 我的某店铺的商品列表 */
    @GetMapping("/shops/{shopId}/products")
    public Result<List<Product>> myProducts(@PathVariable Long shopId, HttpServletRequest req) {
        ownedShop(req, shopId);
        return Result.ok(productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getShopId, shopId)
                .orderByDesc(Product::getCreatedAt)));
    }

    /** 新增商品：同店铺内商品名查重，重名直接拒绝 */
    @PostMapping("/shops/{shopId}/products")
    public Result<Product> addProduct(@PathVariable Long shopId, @RequestBody Product product,
                                      HttpServletRequest req) {
        ownedShop(req, shopId);
        validateProduct(product);
        checkDuplicate(shopId, product.getName(), null);
        product.setShopId(shopId);
        product.setMerchantId(uid(req));
        Shop shop = shopMapper.selectById(shopId);
        product.setSpotId(shop.getSpotId());
        if (product.getStatus() == null) product.setStatus(1);
        if (product.getStock() == null) product.setStock(999);
        if (product.getSales() == null) product.setSales(0);
        productMapper.insert(product);
        return Result.ok(product);
    }

    /** 修改商品（归属校验 + 改名查重） */
    @PutMapping("/products/{id}")
    public Result<Product> updateProduct(@PathVariable Long id, @RequestBody Product body,
                                         HttpServletRequest req) {
        Product product = ownedProduct(req, id);
        if (body.getName() != null && !body.getName().isBlank()) {
            checkDuplicate(product.getShopId(), body.getName().trim(), id);
            product.setName(body.getName().trim());
        }
        if (body.getType() != null) product.setType(body.getType());
        if (body.getPrice() != null) {
            if (body.getPrice().signum() <= 0) throw new BusinessException("价格必须大于 0");
            product.setPrice(body.getPrice());
        }
        if (body.getOriginalPrice() != null) product.setOriginalPrice(body.getOriginalPrice());
        if (body.getCoverImg() != null) product.setCoverImg(body.getCoverImg());
        if (body.getDescription() != null) product.setDescription(body.getDescription());
        if (body.getStock() != null) product.setStock(body.getStock());
        if (body.getStatus() != null) product.setStatus(body.getStatus());
        productMapper.updateById(product);
        return Result.ok(product);
    }

    /** 删除商品 */
    @DeleteMapping("/products/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id, HttpServletRequest req) {
        ownedProduct(req, id);
        productMapper.deleteById(id);
        return Result.ok();
    }

    // ================= 销量统计 =================

    /** 今日/累计营业额 + 订单数 + 商品销量排行 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(HttpServletRequest req) {
        Long mid = uid(req);
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();

        Long todayOrders = orderMapper.selectCount(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getMerchantId, mid)
                .eq(OrderInfo::getStatus, OrderInfo.PAID)
                .ge(OrderInfo::getCreatedAt, dayStart));
        List<OrderInfo> paid = orderMapper.selectList(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getMerchantId, mid)
                .eq(OrderInfo::getStatus, OrderInfo.PAID));
        BigDecimal todayAmount = paid.stream()
                .filter(o -> o.getCreatedAt().isAfter(dayStart))
                .map(OrderInfo::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAmount = paid.stream()
                .map(OrderInfo::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        // 商品销量排行：按「我名下的店铺」反查，只统计自己的商品
        List<Long> myShopIds = shopMapper.selectList(new LambdaQueryWrapper<Shop>()
                        .eq(Shop::getMerchantId, mid))
                .stream().map(Shop::getId).toList();
        List<Product> top = myShopIds.isEmpty() ? List.of()
                : productMapper.selectList(new LambdaQueryWrapper<Product>()
                        .in(Product::getShopId, myShopIds)
                        .orderByDesc(Product::getSales)
                        .last("LIMIT 10"));

        Map<String, Object> data = new HashMap<>();
        data.put("todayOrders", todayOrders);
        data.put("todayAmount", todayAmount);
        data.put("totalAmount", totalAmount);
        data.put("paidOrders", paid.size());
        data.put("topProducts", top);
        return Result.ok(data);
    }

    /** 我（商家）收到的订单 */
    @GetMapping("/orders")
    public Result<IPage<OrderInfo>> myOrders(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) String status,
                                             HttpServletRequest req) {
        LambdaQueryWrapper<OrderInfo> qw = new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getMerchantId, uid(req))
                .orderByDesc(OrderInfo::getCreatedAt);
        if (status != null && !status.isBlank()) qw.eq(OrderInfo::getStatus, status);
        return Result.ok(orderMapper.selectPage(new Page<>(page, size), qw));
    }

    // ================= 私有工具 =================

    private Long uid(HttpServletRequest req) {
        return (Long) req.getAttribute("uid");
    }

    private void requireMerchant(HttpServletRequest req) {
        User user = userMapper.selectById(uid(req));
        if (user == null || !"MERCHANT".equals(user.getRole())) {
            throw new BusinessException("仅商家可执行此操作");
        }
    }

    private Shop ownedShop(HttpServletRequest req, Long shopId) {
        requireMerchant(req);
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null || !shop.getMerchantId().equals(uid(req))) {
            throw new BusinessException("店铺不存在或无权操作");
        }
        return shop;
    }

    private Product ownedProduct(HttpServletRequest req, Long productId) {
        requireMerchant(req);
        Product product = productMapper.selectById(productId);
        if (product == null || product.getShopId() == null) {
            throw new BusinessException("商品不存在或无权操作");
        }
        // 归属一律以「商品所属店铺」为准，不依赖 product.merchant_id 这个冗余字段：
        // 历史数据曾出现商品挂在 A 店铺下、merchant_id 却写成 B，导致老板编辑不了自己的商品。
        Shop shop = shopMapper.selectById(product.getShopId());
        if (shop == null || !uid(req).equals(shop.getMerchantId())) {
            throw new BusinessException("商品不存在或无权操作");
        }
        return product;
    }

    /** 同店铺内商品名查重：excludeId 用于修改时排除自身 */
    private void checkDuplicate(Long shopId, String name, Long excludeId) {
        Long count = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getShopId, shopId)
                .eq(Product::getName, name)
                .ne(excludeId != null, Product::getId, excludeId));
        if (count > 0) throw new BusinessException("该店铺已存在同名商品：「" + name + "」，不能重复添加");
    }

    private void validateProduct(Product p) {
        if (p.getName() == null || p.getName().isBlank()) throw new BusinessException("商品名不能为空");
        if (p.getPrice() == null || p.getPrice().signum() <= 0) throw new BusinessException("价格必须大于 0");
        if (!Product.FOOD.equals(p.getType()) && !Product.DRINK.equals(p.getType())
                && !Product.PACKAGE.equals(p.getType())) {
            throw new BusinessException("商品类型必须是 FOOD / DRINK / PACKAGE");
        }
    }

    @Data
    public static class StatsResult {
        private Long todayOrders;
        private BigDecimal todayAmount;
        private BigDecimal totalAmount;
    }
}
