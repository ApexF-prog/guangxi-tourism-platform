package com.gxtourism.service.impl;

import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.util.ResponseChecker;
import com.alipay.easysdk.payment.common.models.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gxtourism.common.BusinessException;
import com.gxtourism.config.AlipayProperties;
import com.gxtourism.entity.PaymentRecord;
import com.gxtourism.entity.User;
import com.gxtourism.entity.VipOrder;
import com.gxtourism.mapper.PaymentRecordMapper;
import com.gxtourism.mapper.UserMapper;
import com.gxtourism.mapper.VipOrderMapper;
import com.gxtourism.service.VipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class VipServiceImpl implements VipService {

    private final VipOrderMapper vipOrderMapper;
    private final UserMapper userMapper;
    private final PaymentRecordMapper paymentMapper;
    private final AlipayProperties alipay;

    @Override
    public List<Map<String, Object>> plans() {
        List<Map<String, Object>> list = new ArrayList<>();
        // 仿汽水音乐档位：连续包月专享价 < 连续包年 < 12 个月 < 6 个月 < 单月
        list.add(plan(1L, "连续包月", "续费专享优惠", "6.8", "15", 1));
        list.add(plan(2L, "连续包年", "低至 5.7 元/月", "68", "128", 12));
        list.add(plan(3L, "12个月", "单月最低", "88", "128", 12));
        list.add(plan(4L, "6个月", "低至 8 元/月", "48", "64", 6));
        list.add(plan(5L, "1个月", "单月体验", "15", "15", 1));
        return list;
    }

    private Map<String, Object> plan(Long id, String name, String tag, String price, String original, int months) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("tag", tag);
        m.put("price", new BigDecimal(price));
        m.put("original", new BigDecimal(original));
        m.put("months", months);
        return m;
    }

    private Map<String, Object> findPlan(Long planId) {
        return plans().stream().filter(p -> p.get("id").equals(planId))
                .findFirst().orElseThrow(() -> new BusinessException("套餐不存在"));
    }

    @Override
    public Map<String, Object> status(Long userId) {
        User u = userMapper.selectById(userId);
        Map<String, Object> m = new LinkedHashMap<>();
        boolean valid = com.gxtourism.common.VipPolicy.valid(u);
        m.put("vip", valid);
        m.put("vipLevel", u == null ? 0 : u.getVipLevel());
        m.put("vipExpire", u == null ? null : u.getVipExpire());
        m.put("discount", com.gxtourism.common.VipPolicy.DISCOUNT);
        return m;
    }

    @Override
    public Map<String, String> createPayForm(Long userId, Long planId) {
        Map<String, Object> plan = findPlan(planId);
        BigDecimal amount = (BigDecimal) plan.get("price");
        String planName = (String) plan.get("name");

        VipOrder vo = new VipOrder();
        vo.setOrderNo("VIP" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(100, 1000));
        vo.setUserId(userId);
        vo.setPlanId(planId);
        vo.setPlanName(planName);
        vo.setMonths((Integer) plan.get("months"));
        vo.setAmount(amount);
        vo.setStatus(VipOrder.PENDING);
        vipOrderMapper.insert(vo);

        try {
            String form = Factory.Payment.Page()
                    .pay("广西旅游网会员-" + planName, vo.getOrderNo(),
                            amount.toPlainString(), alipay.getReturnUrl())
                    .body;
            Map<String, String> r = new LinkedHashMap<>();
            r.put("orderNo", vo.getOrderNo());
            r.put("form", form);
            return r;
        } catch (Exception e) {
            log.error("生成会员支付表单失败 orderNo={}", vo.getOrderNo(), e);
            throw new BusinessException("支付宝下单失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Map<String, Object> queryAndSync(Long userId, String orderNo) {
        VipOrder vo = vipOrderMapper.selectOne(new LambdaQueryWrapper<VipOrder>()
                .eq(VipOrder::getOrderNo, orderNo));
        if (vo == null || !vo.getUserId().equals(userId)) throw new BusinessException("订单不存在");

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orderNo", orderNo);
        m.put("status", vo.getStatus());
        if (!VipOrder.PENDING.equals(vo.getStatus())) {
            m.put("vipExpire", status(userId).get("vipExpire"));
            return m;
        }

        try {
            AlipayTradeQueryResponse resp = Factory.Payment.Common().query(orderNo);
            if (!ResponseChecker.success(resp)) return m;
            if ("TRADE_SUCCESS".equals(resp.tradeStatus) || "FINISHED".equals(resp.tradeStatus)) {
                BigDecimal paid = new BigDecimal(resp.totalAmount);
                if (paid.compareTo(vo.getAmount()) != 0) {
                    log.error("会员支付金额不一致! orderNo={} 应付={} 实付={}", orderNo, vo.getAmount(), paid);
                    throw new BusinessException("支付金额校验失败，请联系管理员");
                }
                vo.setStatus(VipOrder.PAID);
                vo.setTradeNo(resp.tradeNo);
                vo.setPaidAt(LocalDateTime.now());
                vipOrderMapper.updateById(vo);

                // 延长会员：未过期则在原到期时间上累加，否则从现在起算
                User u = userMapper.selectById(userId);
                LocalDateTime base = com.gxtourism.common.VipPolicy.valid(u) ? u.getVipExpire() : LocalDateTime.now();
                u.setVipLevel(1);
                u.setVipExpire(base.plusMonths(vo.getMonths()));
                userMapper.updateById(u);

                PaymentRecord r = new PaymentRecord();
                r.setOrderNo(orderNo);
                r.setTradeNo(resp.tradeNo == null ? "" : resp.tradeNo);
                r.setAmount(vo.getAmount());
                r.setStatus(resp.tradeStatus);
                paymentMapper.insert(r);
            } else if ("TRADE_CLOSED".equals(resp.tradeStatus)) {
                vo.setStatus(VipOrder.CANCELED);
                vipOrderMapper.updateById(vo);
            }
            m.put("status", vo.getStatus());
            m.put("vipExpire", status(userId).get("vipExpire"));
            return m;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("会员支付查单异常 orderNo={}", orderNo, e);
            throw new BusinessException("查询支付结果失败：" + e.getMessage());
        }
    }
}
