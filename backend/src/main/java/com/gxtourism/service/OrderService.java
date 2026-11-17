package com.gxtourism.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gxtourism.dto.OrderCreateDTO;
import com.gxtourism.entity.OrderInfo;

public interface OrderService {
    /** 下单：金额以后端库价重算，扣减库存，返回订单 */
    OrderInfo create(Long userId, OrderCreateDTO dto);

    /** 我的订单，按创建时间倒序 */
    IPage<OrderInfo> myOrders(Long userId, long page, long size);

    /** 按订单号查详情（校验归属） */
    OrderInfo getByOrderNo(Long userId, String orderNo);

    /** 取消待支付订单（回补库存） */
    void cancel(Long userId, String orderNo);
}
