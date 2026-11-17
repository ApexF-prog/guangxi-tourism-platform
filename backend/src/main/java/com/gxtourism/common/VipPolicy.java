package com.gxtourism.common;

import com.gxtourism.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员（VIP）统一策略：开通会员后全站商品与跟团游报名均享 9 折优惠。
 * 折扣只由后端计算，前端展示的会员价仅供参考。
 */
public final class VipPolicy {

    /** 会员折扣：9 折 */
    public static final BigDecimal DISCOUNT = new BigDecimal("0.90");

    private VipPolicy() {}

    /** 会员是否在有效期内 */
    public static boolean valid(User u) {
        return u != null
                && u.getVipLevel() != null && u.getVipLevel() == 1
                && u.getVipExpire() != null
                && u.getVipExpire().isAfter(LocalDateTime.now());
    }

    /** 按会员策略计算单价（非会员原价，会员 9 折，保留 2 位小数） */
    public static BigDecimal unitPrice(User u, BigDecimal origin) {
        if (valid(u)) return origin.multiply(DISCOUNT).setScale(2, java.math.RoundingMode.HALF_UP);
        return origin;
    }
}
