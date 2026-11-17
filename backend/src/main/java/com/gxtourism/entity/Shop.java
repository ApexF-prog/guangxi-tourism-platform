package com.gxtourism.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Shop {
    public static final String FOOD = "FOOD";
    public static final String DRINK = "DRINK";

    private Long id;
    private Long merchantId;
    private String name;
    private String type;
    private Long spotId;
    private String address;
    private String phone;
    private String coverImg;
    private String description;
    private BigDecimal rating;
    private Integer status;
    private LocalDateTime createdAt;
}
