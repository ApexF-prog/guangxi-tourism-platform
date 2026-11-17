package com.gxtourism.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gxtourism.common.BusinessException;
import com.gxtourism.entity.Product;
import com.gxtourism.entity.ScenicSpot;
import com.gxtourism.mapper.ProductMapper;
import com.gxtourism.mapper.ScenicSpotMapper;
import com.gxtourism.service.SpotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SpotServiceImpl implements SpotService {

    private final ScenicSpotMapper spotMapper;
    private final ProductMapper productMapper;

    @Override
    public IPage<ScenicSpot> page(long page, long size, String keyword, String city, String level) {
        LambdaQueryWrapper<ScenicSpot> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(ScenicSpot::getName, keyword)
                    .or().like(ScenicSpot::getTags, keyword)
                    .or().like(ScenicSpot::getAddress, keyword));
        }
        if (StringUtils.hasText(city)) {
            // 支持城市分组：桂林,阳朔,龙胜
            if (city.contains(",")) {
                qw.in(ScenicSpot::getCity, List.of(city.split(",")));
            } else {
                qw.eq(ScenicSpot::getCity, city);
            }
        }
        if (StringUtils.hasText(level)) qw.eq(ScenicSpot::getLevel, level);
        qw.orderByDesc(ScenicSpot::getHot);
        return spotMapper.selectPage(new Page<>(page, size), qw);
    }

    @Override
    public Map<String, Object> detail(Long id) {
        ScenicSpot spot = spotMapper.selectById(id);
        if (spot == null) throw new BusinessException("景点不存在");
        // 该景区在售商品：直营门票 + 团购套餐
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getSpotId, id)
                .eq(Product::getStatus, 1)
                .in(Product::getType, Product.TICKET, Product.PACKAGE)
                .orderByAsc(Product::getPrice));
        Map<String, Object> data = new HashMap<>();
        data.put("spot", spot);
        data.put("products", products);
        return data;
    }

    @Override
    public List<ScenicSpot> hot(int limit, String city) {
        LambdaQueryWrapper<ScenicSpot> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(city)) {
            if (city.contains(",")) qw.in(ScenicSpot::getCity, List.of(city.split(",")));
            else qw.eq(ScenicSpot::getCity, city);
        }
        qw.orderByDesc(ScenicSpot::getHot);
        return spotMapper.selectPage(new Page<>(1, limit), qw).getRecords();
    }

    @Override
    public List<String> cities() {
        return spotMapper.selectList(null).stream()
                .map(ScenicSpot::getCity).distinct().sorted().toList();
    }
}
