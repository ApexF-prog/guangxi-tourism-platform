package com.gxtourism.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ScenicSpot {
    private Long id;
    private String name;
    private String city;
    private String level;
    private String coverImg;
    private BigDecimal price;
    private BigDecimal rating;
    private String tags;
    private String address;
    private String description;
    private Integer hot;
    private LocalDateTime createdAt;
}
