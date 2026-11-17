package com.gxtourism.controller;

import com.gxtourism.common.Result;
import com.gxtourism.entity.OrderInfo;
import com.gxtourism.service.PayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/pay/alipay")
@RequiredArgsConstructor
public class PayController {

    private final PayService payService;

    /** 生成沙箱支付表单（前端写入新窗口自动跳转支付宝） */
    @PostMapping("/create")
    public Result<String> create(@RequestBody CreateDTO dto, HttpServletRequest req) {
        return Result.ok(payService.createAlipayForm((Long) req.getAttribute("uid"), dto.getOrderNo()));
    }

    /**
     * 直达沙箱收银台登录页：服务端预演表单提交，返回 {mode, target}。
     * redirect = 直接跳转登录页 URL（推荐，跳过中间页）；form = 原始自动提交表单兜底。
     */
    @PostMapping("/cashier")
    public Result<Map<String, String>> cashier(@RequestBody CreateDTO dto, HttpServletRequest req) {
        return Result.ok(payService.createCashier((Long) req.getAttribute("uid"), dto.getOrderNo()));
    }

    /** 主动查单并同步订单状态（前端支付后轮询，作为异步通知的兜底） */
    @GetMapping("/query/{orderNo}")
    public Result<OrderInfo> query(@PathVariable String orderNo, HttpServletRequest req) {
        return Result.ok(payService.queryAndSync((Long) req.getAttribute("uid"), orderNo));
    }

    /**
     * 服务端异步通知（支付宝服务器直连，无登录态，不在拦截器白名单之外）。
     * 必须返回纯文本 success，否则支付宝会按策略重复推送。
     * 上线时把 ALIPAY_NOTIFY_URL 配成公网 https 地址。
     */
    @PostMapping(value = "/notify", produces = "text/plain;charset=UTF-8")
    public String notify(HttpServletRequest req) {
        Map<String, String> params = new HashMap<>();
        req.getParameterMap().forEach((k, v) -> params.put(k, v.length > 0 ? v[0] : ""));
        return payService.handleNotify(params);
    }

    @Data
    public static class CreateDTO {
        private String orderNo;
    }
}
