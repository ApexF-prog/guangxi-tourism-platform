package com.gxtourism.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gxtourism.common.BusinessException;
import com.gxtourism.common.Result;
import com.gxtourism.entity.OrderInfo;
import com.gxtourism.entity.Product;
import com.gxtourism.entity.Review;
import com.gxtourism.entity.User;
import com.gxtourism.mapper.OrderInfoMapper;
import com.gxtourism.mapper.ProductMapper;
import com.gxtourism.mapper.ReviewMapper;
import com.gxtourism.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    /** 每个用户对同一商品最多可发表的评价条数（防刷评） */
    private static final int MAX_REVIEWS_PER_PRODUCT = 2;

    private final ReviewMapper reviewMapper;
    private final ProductMapper productMapper;
    private final OrderInfoMapper orderMapper;
    private final UserMapper userMapper;

    /** 商品评价列表（昵称脱敏显示） */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(@RequestParam Long productId) {
        List<Review> reviews = reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getProductId, productId)
                .orderByDesc(Review::getCreatedAt));
        List<Map<String, Object>> data = new ArrayList<>();
        for (Review r : reviews) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("rating", r.getRating());
            m.put("content", r.getContent());
            m.put("createdAt", r.getCreatedAt());
            User u = userMapper.selectById(r.getUserId());
            String nickname = u == null ? "匿名用户" : u.getNickname();
            if (nickname.length() > 2) nickname = nickname.charAt(0) + "**";
            m.put("nickname", nickname);
            m.put("avatar", u == null ? "" : u.getAvatar());
            data.add(m);
        }
        return Result.ok(data);
    }

    /** 发表评价：必须购买过（有该商品 PAID 订单），且每商品每用户最多 2 条 */
    @PostMapping
    public Result<Void> add(@RequestBody ReviewDTO dto, HttpServletRequest req) {
        Long uid = (Long) req.getAttribute("uid");
        Product product = productMapper.selectById(dto.getProductId());
        if (product == null) throw new BusinessException("商品不存在");

        // 1. 必须买过：存在该用户 + 该商品 的已支付订单
        Long paidOrders = orderMapper.selectCount(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getUserId, uid)
                .eq(OrderInfo::getProductId, dto.getProductId())
                .eq(OrderInfo::getStatus, OrderInfo.PAID));
        if (paidOrders == 0) throw new BusinessException("购买后才能评价");

        // 2. 防刷分：同一商品每人最多 2 条评价
        Long count = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getUserId, uid)
                .eq(Review::getProductId, dto.getProductId()));
        if (count >= MAX_REVIEWS_PER_PRODUCT) {
            throw new BusinessException("每个商品最多评价 " + MAX_REVIEWS_PER_PRODUCT + " 次");
        }

        if (dto.getRating() == null || dto.getRating() < 1 || dto.getRating() > 5) {
            throw new BusinessException("评分 1-5 星");
        }
        if (dto.getContent() == null || dto.getContent().isBlank()) {
            throw new BusinessException("评价内容不能为空");
        }
        Review review = new Review();
        review.setUserId(uid);
        review.setProductId(dto.getProductId());
        review.setShopId(product.getShopId());
        review.setRating(dto.getRating());
        review.setContent(dto.getContent().trim());
        reviewMapper.insert(review);
        return Result.ok();
    }

    @Data
    public static class ReviewDTO {
        private Long productId;
        private Integer rating;
        private String content;
    }
}
