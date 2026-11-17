package com.gxtourism.service;

import com.gxtourism.entity.VipOrder;

import java.util.List;
import java.util.Map;

public interface VipService {

    /** 会员套餐列表（仿汽水音乐会员档位） */
    List<Map<String, Object>> plans();

    /** 当前用户会员状态 */
    Map<String, Object> status(Long userId);

    /** 创建会员开通订单，返回支付表单 HTML + 订单号（前端凭单号轮询） */
    Map<String, String> createPayForm(Long userId, Long planId);

    /** 查询支付结果并同步会员状态（幂等） */
    Map<String, Object> queryAndSync(Long userId, String orderNo);
}
