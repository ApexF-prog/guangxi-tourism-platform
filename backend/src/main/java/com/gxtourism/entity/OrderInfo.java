package com.gxtourism.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_order")
public class OrderInfo {
    public static final String PENDING = "PENDING";
    public static final String PAID = "PAID";
    public static final String CANCELED = "CANCELED";

    private Long id;
    private String orderNo;
    private Long userId;
    private Long productId;
    private String productName;
    private String productType;
    private Long shopId;
    private Long merchantId;
    private Long spotId;
    private String spotName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal total;
    private LocalDate visitDate;
    private String status;
    private String payType;
    private String tradeNo;
    /** 取餐号（美食/饮品订单，支付成功后可凭号取餐） */
    private String pickNo;
    /** 1 = 本单按会员价（9 折）结算 */
    private Integer vipPrice;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
