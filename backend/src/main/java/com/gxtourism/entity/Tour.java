package com.gxtourism.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 跟团游线路（只做广西：桂林/柳州/南宁/北海） */
@Data
@TableName("tour")
public class Tour {
    private Long id;
    private String city;
    private String title;
    private String coverImg;
    private Integer days;
    /** 出发城市 */
    private String departure;
    /** 亮点，逗号分隔 */
    private String highlights;
    /** 行程安排文字 */
    private String itinerary;
    /** 成人原价 */
    private BigDecimal price;
    private Integer stock;
    private Integer status;
    private LocalDateTime createdAt;
}
