package com.gxtourism.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class OrderCreateDTO {
    private Long productId;
    private Integer quantity;
    /** 门票/套餐需要游玩日期；餐饮饮品可空 */
    private LocalDate visitDate;
}
