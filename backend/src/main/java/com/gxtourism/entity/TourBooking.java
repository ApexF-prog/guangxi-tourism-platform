package com.gxtourism.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 跟团游报名单 */
@Data
@TableName("tour_booking")
public class TourBooking {
    public static final String PENDING = "PENDING";
    public static final String PAID = "PAID";
    public static final String CANCELED = "CANCELED";

    private Long id;
    private String bookingNo;
    private Long userId;
    private Long tourId;
    private String tourTitle;
    private LocalDate travelDate;
    private Integer people;
    /** 成交单价（会员 9 折后） */
    private BigDecimal unitPrice;
    private BigDecimal total;
    private String contactPhone;
    private String status;
    private String payType;
    private String tradeNo;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
