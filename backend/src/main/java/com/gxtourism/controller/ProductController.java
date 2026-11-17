package com.gxtourism.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gxtourism.common.BusinessException;
import com.gxtourism.common.Result;
import com.gxtourism.entity.PackageItem;
import com.gxtourism.entity.Product;
import com.gxtourism.entity.Review;
import com.gxtourism.mapper.PackageItemMapper;
import com.gxtourism.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductMapper productMapper;
    private final PackageItemMapper packageItemMapper;
    private final com.gxtourism.mapper.ScenicSpotMapper spotMapper;
    private final com.gxtourism.mapper.ShopMapper shopMapper;

    /** 首页推荐：销量 top（可按城市分组过滤：桂林,阳朔,龙胜） */
    @GetMapping("/hot")
    public Result<List<Product>> hot(@RequestParam(defaultValue = "8") int limit,
                                     @RequestParam(required = false) String city) {
        LambdaQueryWrapper<Product> qw = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1)
                .in(Product::getType, Product.FOOD, Product.DRINK, Product.PACKAGE);
        if (city != null && !city.isBlank()) {
            List<String> cities = List.of(city.split(","));
            List<Long> spotIds = spotMapper.selectList(new LambdaQueryWrapper<com.gxtourism.entity.ScenicSpot>()
                    .in(com.gxtourism.entity.ScenicSpot::getCity, cities)).stream()
                    .map(com.gxtourism.entity.ScenicSpot::getId).toList();
            List<Long> shopIds = shopMapper.selectList(new LambdaQueryWrapper<com.gxtourism.entity.Shop>()
                    .in(com.gxtourism.entity.Shop::getSpotId, spotIds.isEmpty() ? List.of(-1L) : spotIds)).stream()
                    .map(com.gxtourism.entity.Shop::getId).toList();
            qw.and(w -> w.in(!spotIds.isEmpty(), Product::getSpotId, spotIds)
                    .or().in(!shopIds.isEmpty(), Product::getShopId, shopIds));
        }
        qw.orderByDesc(Product::getSales);
        List<Product> list = productMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, limit), qw).getRecords();
        return Result.ok(list);
    }

    /** 商品详情（套餐返回组成明细） */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) throw new BusinessException("商品不存在");
        Map<String, Object> data = new HashMap<>();
        data.put("product", product);
        if (Product.PACKAGE.equals(product.getType())) {
            List<Map<String, Object>> items = packageItemMapper.selectList(
                            new LambdaQueryWrapper<PackageItem>().eq(PackageItem::getPackageId, id))
                    .stream().map(pi -> {
                        Map<String, Object> m = new HashMap<>();
                        Product p = productMapper.selectById(pi.getProductId());
                        m.put("name", p == null ? "" : p.getName());
                        m.put("price", p == null ? null : p.getPrice());
                        m.put("quantity", pi.getQuantity());
                        return m;
                    }).toList();
            data.put("items", items);
        }
        return Result.ok(data);
    }
}
