package com.gxtourism.service.impl;

import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.util.ResponseChecker;
import com.alipay.easysdk.payment.common.models.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.gxtourism.common.BusinessException;
import com.gxtourism.config.AlipayProperties;
import com.gxtourism.entity.OrderInfo;
import com.gxtourism.entity.PaymentRecord;
import com.gxtourism.entity.Product;
import com.gxtourism.mapper.OrderInfoMapper;
import com.gxtourism.mapper.PaymentRecordMapper;
import com.gxtourism.mapper.ProductMapper;
import com.gxtourism.service.PayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayServiceImpl implements PayService {

    private final OrderInfoMapper orderMapper;
    private final PaymentRecordMapper paymentMapper;
    private final ProductMapper productMapper;
    private final AlipayProperties alipay;

    @Override
    public String createAlipayForm(Long userId, String orderNo) {
        OrderInfo order = orderMapper.selectOne(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getOrderNo, orderNo));
        if (order == null || !order.getUserId().equals(userId)) throw new BusinessException("订单不存在");
        if (!OrderInfo.PENDING.equals(order.getStatus())) throw new BusinessException("订单不是待支付状态");

        try {
            String subject = order.getProductName() + " x" + order.getQuantity();
            String amount = order.getTotal().toPlainString();
            String channel = alipay.getChannel() == null ? "wap" : alipay.getChannel().trim().toLowerCase();
            String form;
            if ("page".equals(channel)) {
                // 电脑网站支付：沙箱维护窗口内网关常返回 ACQ.SYSTEM_ERROR，可通过 ALIPAY_CHANNEL=page 切回
                com.alipay.easysdk.payment.page.Client client = Factory.Payment.Page();
                applyNotify(client::asyncNotify);
                form = client.pay(subject, order.getOrderNo(), amount, alipay.getReturnUrl()).body;
            } else {
                // 手机网站支付：同一把商户私钥签名、同一套查单/通知接口，沙箱收银台当前稳定可用
                com.alipay.easysdk.payment.wap.Client client = Factory.Payment.Wap();
                applyNotify(client::asyncNotify);
                String quitUrl = (alipay.getQuitUrl() == null || alipay.getQuitUrl().isBlank())
                        ? alipay.getReturnUrl() : alipay.getQuitUrl();
                form = client.pay(subject, order.getOrderNo(), amount, quitUrl, alipay.getReturnUrl()).body;
            }
            log.info("生成支付宝收银台表单 orderNo={} channel={} bytes={}", orderNo, channel, form.length());
            // 返回一段自动提交的 HTML form，写入浏览器即跳转沙箱收银台
            return form;
        } catch (Exception e) {
            log.error("生成支付宝支付表单失败 orderNo={}", orderNo, e);
            throw new BusinessException("支付宝下单失败：" + e.getMessage());
        }
    }

    /** pay() 不会自动读全局 config.notifyUrl，必须显式 asyncNotify 才会带上 notify_url */
    private void applyNotify(java.util.function.Consumer<String> asyncNotify) {
        if (alipay.getNotifyUrl() != null && !alipay.getNotifyUrl().isBlank()) {
            asyncNotify.accept(alipay.getNotifyUrl());
        }
    }

    @Override
    public Map<String, String> createCashier(Long userId, String orderNo) {
        String form = createAlipayForm(userId, orderNo);
        try {
            // 解析自己生成的自动提交表单，在服务端预演一次「表单 -> 中间页」，
            // 提取「继续浏览器付款」按钮背后的 h5Continue 登录页地址。
            // 沙箱 mobilepay 中间页在 PC 浏览器上按钮 JS 常不生效，直达登录页可彻底跳过它。
            java.util.regex.Matcher mAction = java.util.regex.Pattern
                    .compile("action\\s*=\\s*\"([^\"]+)\"").matcher(form);
            java.util.regex.Matcher mBiz = java.util.regex.Pattern
                    .compile("name=\"biz_content\"\\s+value=\"([^\"]*)\"").matcher(form);
            if (mAction.find() && mBiz.find()) {
                String action = mAction.group(1).replace("&amp;", "&");
                String bizContent = mBiz.group(1).replace("&quot;", "\"").replace("&amp;", "&");
                java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                        .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                        .connectTimeout(java.time.Duration.ofSeconds(8))
                        .build();
                java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(action))
                        .timeout(java.time.Duration.ofSeconds(12))
                        .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                                + "(KHTML, like Gecko) Chrome/126.0 Safari/537.36")
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(
                                "biz_content=" + java.net.URLEncoder.encode(bizContent, java.nio.charset.StandardCharsets.UTF_8),
                                java.nio.charset.StandardCharsets.UTF_8))
                        .build();
                // h5payurl 值为纯 ASCII（token），编码不影响提取
                java.net.http.HttpResponse<String> resp = client.send(req,
                        java.net.http.HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.ISO_8859_1));
                String body = resp.body();
                // 优先提取「支付密码登录」页地址：沙箱买家账号只支持账号密码登录，
                // 默认的 phoneLogin 手机号登录页对沙箱账号一律报「系统繁忙」，必须绕开。
                java.util.regex.Matcher mPwd = java.util.regex.Pattern
                        .compile("(https?://[^\"']*loginIdPwdToLogin\\.htm[^\"']*)").matcher(body);
                if (mPwd.find()) {
                    String pwdUrl = mPwd.group(1).replace("&amp;", "&");
                    log.info("收银台直达（账号密码登录页）成功 orderNo={} target={}", orderNo, pwdUrl);
                    return Map.of("mode", "redirect", "target", pwdUrl);
                }
                // 兜底：h5Continue 登录选择页（页内有「支付密码登录」切换入口）
                java.util.regex.Matcher mUrl = java.util.regex.Pattern
                        .compile("h5payurl\\s*=\\s*\"([^\"]+)\"").matcher(body);
                if (mUrl.find()) {
                    String target = resp.uri().resolve(mUrl.group(1)).toString();
                    // 第二跳：GET h5Continue 登录选择页，提取「支付密码登录」页地址。
                    // 沙箱买家账号只支持账号密码登录，默认手机号登录页对沙箱账号报「系统繁忙」。
                    try {
                        java.net.http.HttpRequest req2 = java.net.http.HttpRequest.newBuilder()
                                .uri(java.net.URI.create(target))
                                .timeout(java.time.Duration.ofSeconds(12))
                                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                                        + "(KHTML, like Gecko) Chrome/126.0 Safari/537.36")
                                .GET().build();
                        java.net.http.HttpResponse<String> resp2 = client.send(req2,
                                java.net.http.HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.ISO_8859_1));
                        java.util.regex.Matcher mPwd2 = java.util.regex.Pattern
                                .compile("(https?://[^\"']*loginIdPwdToLogin\\.htm[^\"']*)").matcher(resp2.body());
                        if (mPwd2.find()) {
                            String pwdUrl = mPwd2.group(1).replace("&amp;", "&");
                            log.info("收银台直达（账号密码登录页，第二跳）成功 orderNo={}", orderNo);
                            return Map.of("mode", "redirect", "target", pwdUrl);
                        }
                    } catch (Exception ignore) {
                        log.warn("第二跳提取账号密码登录页失败，回退 h5Continue orderNo={}: {}", orderNo, ignore.getMessage());
                    }
                    log.info("收银台直达预演成功 orderNo={} target={}", orderNo, target);
                    return Map.of("mode", "redirect", "target", target);
                }
                log.warn("收银台响应未包含登录页链接，回退表单模式 orderNo={} bytes={}", orderNo, body.length());
            } else {
                log.warn("支付表单解析失败，回退表单模式 orderNo={}", orderNo);
            }
        } catch (Exception e) {
            log.warn("收银台直达预演异常，回退表单模式 orderNo={}: {}", orderNo, e.getMessage());
        }
        return Map.of("mode", "form", "target", form);
    }

    @Override
    public OrderInfo queryAndSync(Long userId, String orderNo) {
        OrderInfo order = orderMapper.selectOne(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getOrderNo, orderNo));
        if (order == null || !order.getUserId().equals(userId)) throw new BusinessException("订单不存在");

        // 已是终态直接返回（幂等：重复查询不会重复处理）
        if (!OrderInfo.PENDING.equals(order.getStatus())) return order;

        try {
            AlipayTradeQueryResponse resp = Factory.Payment.Common().query(orderNo);
            if (!ResponseChecker.success(resp)) {
                // 交易不存在（如用户从未打开收银台）保持待支付
                log.info("支付宝查单未成功 orderNo={} code={} msg={}", orderNo, resp.code, resp.subMsg);
                return order;
            }
            if ("TRADE_SUCCESS".equals(resp.tradeStatus) || "FINISHED".equals(resp.tradeStatus)) {
                // 金额核验：以后端订单为准，防止任何环节金额被篡改
                BigDecimal paid = new BigDecimal(resp.totalAmount);
                if (paid.compareTo(order.getTotal()) != 0) {
                    log.error("支付金额不一致! orderNo={} 库存应付={} 实付={}", orderNo, order.getTotal(), paid);
                    throw new BusinessException("支付金额校验失败，请联系管理员");
                }
                order.setStatus(OrderInfo.PAID);
                order.setTradeNo(resp.tradeNo);
                order.setPaidAt(LocalDateTime.now());
                orderMapper.updateById(order);
                // 累加商品销量（商家后台统计用）
                LambdaUpdateWrapper<Product> suw = new LambdaUpdateWrapper<>();
                suw.eq(Product::getId, order.getProductId())
                   .setSql("sales = sales + " + order.getQuantity());
                productMapper.update(null, suw);
                saveRecord(order, resp.tradeNo, resp.tradeStatus);
            } else if ("TRADE_CLOSED".equals(resp.tradeStatus)) {
                order.setStatus(OrderInfo.CANCELED);
                orderMapper.updateById(order);
                saveRecord(order, resp.tradeNo, resp.tradeStatus);
            }
            return order;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("支付宝查单异常 orderNo={}", orderNo, e);
            throw new BusinessException("查询支付结果失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public String handleNotify(Map<String, String> params) {
        try {
            // 1) 官方验签：RSA2 + 支付宝公钥，失败直接拒绝
            if (!Boolean.TRUE.equals(Factory.Payment.Common().verifyNotify(params))) {
                log.warn("支付宝异步通知验签失败 outTradeNo={}", params.get("out_trade_no"));
                return "failure";
            }
            // 2) 校验收款应用，防止别的商户交易打进来
            String bizAppId = params.get("app_id");
            if (bizAppId != null && !bizAppId.isBlank() && !bizAppId.equals(alipay.getAppId())) {
                log.warn("异步通知 appId 不匹配: {} != {}", bizAppId, alipay.getAppId());
                return "failure";
            }
            String tradeStatus = params.get("trade_status");
            if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
                log.info("异步通知非支付成功状态，忽略: {} / {}", params.get("out_trade_no"), tradeStatus);
                return "success";
            }

            String orderNo = params.get("out_trade_no");
            OrderInfo order = orderMapper.selectOne(new LambdaQueryWrapper<OrderInfo>()
                    .eq(OrderInfo::getOrderNo, orderNo));
            if (order == null) {
                // 会员订单 / 跟团游报名不落在 t_order，由前端主动查单兜底同步
                log.info("异步通知非门票订单（会员/跟团游由轮询同步）: {}", orderNo);
                return "success";
            }

            // 3) 金额核验：以后端订单金额为准
            BigDecimal paid;
            try {
                paid = new BigDecimal(params.get("total_amount"));
            } catch (Exception e) {
                log.error("异步通知金额格式异常 orderNo={} amount={}", orderNo, params.get("total_amount"));
                return "failure";
            }
            if (paid.compareTo(order.getTotal()) != 0) {
                log.error("异步通知金额不一致 orderNo={} 应付={} 实付={}", orderNo, order.getTotal(), paid);
                return "failure";
            }

            // 4) 条件更新实现幂等：只有 PENDING -> PAID 成功才算本次入账，
            //    重复通知 / 前端轮询抢先处理都会命中 n=0，不会重复加销量、重复写流水
            String tradeNo = params.get("trade_no") == null ? "" : params.get("trade_no");
            int n = orderMapper.update(null, new LambdaUpdateWrapper<OrderInfo>()
                    .eq(OrderInfo::getId, order.getId())
                    .eq(OrderInfo::getStatus, OrderInfo.PENDING)
                    .set(OrderInfo::getStatus, OrderInfo.PAID)
                    .set(OrderInfo::getTradeNo, tradeNo)
                    .set(OrderInfo::getPaidAt, LocalDateTime.now()));
            if (n == 0) {
                log.info("异步通知重复到达，幂等跳过: {}", orderNo);
                return "success";
            }

            productMapper.update(null, new LambdaUpdateWrapper<Product>()
                    .eq(Product::getId, order.getProductId())
                    .setSql("sales = sales + " + order.getQuantity()));
            saveRecord(order, tradeNo, tradeStatus);
            log.info("支付宝异步通知已入账 orderNo={} amount={}", orderNo, paid);
            return "success";
        } catch (Exception e) {
            log.error("处理支付宝异步通知异常", e);
            return "failure";
        }
    }

    private void saveRecord(OrderInfo order, String tradeNo, String status) {
        PaymentRecord r = new PaymentRecord();
        r.setOrderNo(order.getOrderNo());
        r.setTradeNo(tradeNo == null ? "" : tradeNo);
        r.setAmount(order.getTotal());
        r.setStatus(status);
        paymentMapper.insert(r);
    }
}
