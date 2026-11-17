package com.gxtourism.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {
    public static final String TICKET = "TICKET";
    public static final String FOOD = "FOOD";
    public static final String DRINK = "DRINK";
    public static final String PACKAGE = "PACKAGE";

    private Long id;
    private Long merchantId;
    private Long shopId;
    private Long spotId;
    private String name;
    private String type;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String coverImg;
    private String description;
    private Integer stock;
    private Integer sales;
    private Integer status;
    private LocalDateTime createdAt;
}
