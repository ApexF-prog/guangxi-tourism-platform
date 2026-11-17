package com.gxtourism.service;

import com.gxtourism.entity.OrderInfo;

import java.util.Map;

public interface PayService {
    /** 生成支付宝沙箱支付页表单 HTML（前端写入新窗口自动提交跳转） */
    String createAlipayForm(Long userId, String orderNo);

    /**
     * 直达收银台：服务端预演 wap 表单提交，拿到「继续浏览器付款」背后的
     * h5Continue 登录页地址后返回 redirect 模式，跳过沙箱中间页；
     * 预演失败时降级 form 模式（返回原始自动提交表单）。
     * 返回 {mode: redirect|form, target: URL 或表单 HTML}
     */
    java.util.Map<String, String> createCashier(Long userId, String orderNo);

    /** 主动查单：校验验签结果 + 金额，幂等更新订单状态，返回最新订单 */
    OrderInfo queryAndSync(Long userId, String orderNo);

    /**
     * 支付宝服务端异步通知：验签 -> 校验 appId / 金额 -> 条件更新保证幂等
     * -> 累加销量 / 写支付流水。返回 "success" 或 "failure" 交给支付宝决定是否重试。
     */
    String handleNotify(Map<String, String> params);
}
