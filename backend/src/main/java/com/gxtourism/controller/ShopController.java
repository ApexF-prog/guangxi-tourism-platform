package com.gxtourism.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gxtourism.common.BusinessException;
import com.gxtourism.common.Result;
import com.gxtourism.entity.Product;
import com.gxtourism.entity.ScenicSpot;
import com.gxtourism.entity.Shop;
import com.gxtourism.mapper.ProductMapper;
import com.gxtourism.mapper.ScenicSpotMapper;
import com.gxtourism.mapper.ShopMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopMapper shopMapper;
    private final ProductMapper productMapper;
    private final ScenicSpotMapper spotMapper;

    /** 店铺列表：?type=FOOD|DRINK&spotId=&city=桂林（支持 桂林,阳朔,龙胜 分组） */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) String type,
                                                  @RequestParam(required = false) Long spotId,
                                                  @RequestParam(required = false) String city) {
        LambdaQueryWrapper<Shop> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(type)) qw.eq(Shop::getType, type);
        if (spotId != null) qw.eq(Shop::getSpotId, spotId);
        if (StringUtils.hasText(city)) {
            List<Long> spotIds = spotMapper.selectList(new LambdaQueryWrapper<ScenicSpot>()
                    .in(ScenicSpot::getCity, List.of(city.split(",")))).stream()
                    .map(ScenicSpot::getId).toList();
            qw.in(Shop::getSpotId, spotIds.isEmpty() ? List.of(-1L) : spotIds);
        }
        qw.eq(Shop::getStatus, 1).orderByDesc(Shop::getRating);
        List<Shop> shops = shopMapper.selectList(qw);
        List<Map<String, Object>> data = shops.stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("name", s.getName());
            m.put("type", s.getType());
            m.put("address", s.getAddress());
            m.put("phone", s.getPhone());
            m.put("coverImg", s.getCoverImg());
            m.put("description", s.getDescription());
            m.put("rating", s.getRating());
            if (s.getSpotId() != null) {
                ScenicSpot spot = spotMapper.selectById(s.getSpotId());
                m.put("spotName", spot == null ? "" : spot.getName());
                m.put("city", spot == null ? "" : spot.getCity());
            }
            return m;
        }).toList();
        return Result.ok(data);
    }

    /** 店铺详情 + 在售商品（餐饮/饮品/套餐） */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Shop shop = shopMapper.selectById(id);
        if (shop == null) throw new BusinessException("店铺不存在");
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getShopId, id)
                .eq(Product::getStatus, 1)
                .orderByDesc(Product::getSales));
        ScenicSpot spot = shop.getSpotId() == null ? null : spotMapper.selectById(shop.getSpotId());
        Map<String, Object> data = new HashMap<>();
        data.put("shop", shop);
        data.put("products", products);
        data.put("spotName", spot == null ? "" : spot.getName());
        return Result.ok(data);
    }
}
