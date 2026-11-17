package com.gxtourism.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 会员开通订单（走支付宝沙箱支付） */
@Data
@TableName("vip_order")
public class VipOrder {
    public static final String PENDING = "PENDING";
    public static final String PAID = "PAID";
    public static final String CANCELED = "CANCELED";

    private Long id;
    private String orderNo;
    private Long userId;
    private Long planId;
    private String planName;
    /** 开通月数 */
    private Integer months;
    private BigDecimal amount;
    private String status;
    private String tradeNo;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
