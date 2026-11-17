package com.gxtourism.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("favorite")
public class Favorite {
    public static final String SPOT = "SPOT";
    public static final String PRODUCT = "PRODUCT";
    public static final String TOUR = "TOUR";
    public static final String SHOP = "SHOP";

    private Long id;
    private Long userId;
    private String itemType;
    private Long itemId;
    private LocalDateTime createdAt;
}
