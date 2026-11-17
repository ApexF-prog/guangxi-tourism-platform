package com.gxtourism.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gxtourism.common.Result;
import com.gxtourism.dto.OrderCreateDTO;
import com.gxtourism.entity.OrderInfo;
import com.gxtourism.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    private Long uid(HttpServletRequest req) {
        return (Long) req.getAttribute("uid");
    }

    @PostMapping("/create")
    public Result<OrderInfo> create(@RequestBody OrderCreateDTO dto, HttpServletRequest req) {
        return Result.ok(orderService.create(uid(req), dto));
    }

    @GetMapping("/my")
    public Result<IPage<OrderInfo>> my(@RequestParam(defaultValue = "1") long page,
                                       @RequestParam(defaultValue = "10") long size,
                                       HttpServletRequest req) {
        return Result.ok(orderService.myOrders(uid(req), page, size));
    }

    @GetMapping("/{orderNo}")
    public Result<OrderInfo> detail(@PathVariable String orderNo, HttpServletRequest req) {
        return Result.ok(orderService.getByOrderNo(uid(req), orderNo));
    }

    @PostMapping("/{orderNo}/cancel")
    public Result<Void> cancel(@PathVariable String orderNo, HttpServletRequest req) {
        orderService.cancel(uid(req), orderNo);
        return Result.ok();
    }
}
