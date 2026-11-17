package com.gxtourism.controller;

import com.gxtourism.common.Result;
import com.gxtourism.service.VipService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vip")
@RequiredArgsConstructor
public class VipController {

    private final VipService vipService;

    /** 会员套餐（公开） */
    @GetMapping("/plans")
    public Result<List<Map<String, Object>>> plans() {
        return Result.ok(vipService.plans());
    }

    /** 我的会员状态 */
    @GetMapping("/status")
    public Result<Map<String, Object>> status(HttpServletRequest req) {
        return Result.ok(vipService.status((Long) req.getAttribute("uid")));
    }

    /** 开通会员：生成支付宝沙箱支付表单（返回 form + orderNo） */
    @PostMapping("/create")
    public Result<Map<String, String>> create(@RequestBody CreateDTO dto, HttpServletRequest req) {
        return Result.ok(vipService.createPayForm((Long) req.getAttribute("uid"), dto.getPlanId()));
    }

    /** 支付结果查询（前端轮询） */
    @GetMapping("/query/{orderNo}")
    public Result<Map<String, Object>> query(@PathVariable String orderNo, HttpServletRequest req) {
        return Result.ok(vipService.queryAndSync((Long) req.getAttribute("uid"), orderNo));
    }

    @Data
    public static class CreateDTO {
        private Long planId;
    }
}
