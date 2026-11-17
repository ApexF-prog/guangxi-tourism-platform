package com.gxtourism.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("review")
public class Review {
    private Long id;
    private Long userId;
    private Long productId;
    private Long shopId;
    private String orderNo;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;
}
