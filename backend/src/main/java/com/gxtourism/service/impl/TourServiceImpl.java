package com.gxtourism.service.impl;

import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.util.ResponseChecker;
import com.alipay.easysdk.payment.common.models.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.gxtourism.common.BusinessException;
import com.gxtourism.common.VipPolicy;
import com.gxtourism.config.AlipayProperties;
import com.gxtourism.entity.PaymentRecord;
import com.gxtourism.entity.Tour;
import com.gxtourism.entity.TourBooking;
import com.gxtourism.entity.User;
import com.gxtourism.mapper.PaymentRecordMapper;
import com.gxtourism.mapper.TourBookingMapper;
import com.gxtourism.mapper.TourMapper;
import com.gxtourism.mapper.UserMapper;
import com.gxtourism.service.TourService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourServiceImpl implements TourService {

    /** 官方咨询渠道 */
    public static final String PHONE = "19175657645";
    public static final String WECHAT = "waxrl0911";

    private final TourMapper tourMapper;
    private final TourBookingMapper bookingMapper;
    private final UserMapper userMapper;
    private final PaymentRecordMapper paymentMapper;
    private final AlipayProperties alipay;

    @Override
    public List<Tour> list(String city) {
        LambdaQueryWrapper<Tour> qw = new LambdaQueryWrapper<Tour>().eq(Tour::getStatus, 1);
        if (city != null && !city.isBlank() && !"全部".equals(city)) qw.eq(Tour::getCity, city);
        qw.orderByAsc(Tour::getDays).orderByAsc(Tour::getPrice);
        return tourMapper.selectList(qw);
    }

    @Override
    public Tour detail(Long id) {
        Tour t = tourMapper.selectById(id);
        if (t == null || t.getStatus() != 1) throw new BusinessException("线路不存在或已下架");
        return t;
    }

    @Override
    @Transactional
    public TourBooking book(Long userId, Long tourId, String travelDate, Integer people, String contactPhone) {
        Tour tour = detail(tourId);
        if (people == null || people < 1 || people > 9) throw new BusinessException("出行人数需在 1-9 人之间");
        if (contactPhone == null || !contactPhone.matches("1[3-9]\\d{9}")) {
            throw new BusinessException("请填写正确的 11 位联系电话");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(travelDate);
        } catch (Exception e) {
            throw new BusinessException("请选择出发日期");
        }
        if (date.isBefore(LocalDate.now())) throw new BusinessException("出发日期需在今天或之后");

        // 并发安全扣名额
        LambdaUpdateWrapper<Tour> uw = new LambdaUpdateWrapper<>();
        uw.eq(Tour::getId, tour.getId())
          .setSql("stock = stock - " + people)
          .ge(Tour::getStock, people);
        if (tourMapper.update(null, uw) == 0) throw new BusinessException("该团余位不足，请联系客服加团");

        User u = userMapper.selectById(userId);
        BigDecimal unit = VipPolicy.unitPrice(u, tour.getPrice());

        TourBooking b = new TourBooking();
        b.setBookingNo("TV" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(100, 1000));
        b.setUserId(userId);
        b.setTourId(tour.getId());
        b.setTourTitle(tour.getTitle());
        b.setTravelDate(date);
        b.setPeople(people);
        b.setUnitPrice(unit);
        b.setTotal(unit.multiply(BigDecimal.valueOf(people)));
        b.setContactPhone(contactPhone);
        b.setStatus(TourBooking.PENDING);
        b.setPayType("ALIPAY");
        bookingMapper.insert(b);
        return b;
    }

    @Override
    public List<TourBooking> my(Long userId) {
        return bookingMapper.selectList(new LambdaQueryWrapper<TourBooking>()
                .eq(TourBooking::getUserId, userId)
                .orderByDesc(TourBooking::getCreatedAt));
    }

    @Override
    public TourBooking getBooking(Long userId, String bookingNo) {
        TourBooking b = bookingMapper.selectOne(new LambdaQueryWrapper<TourBooking>()
                .eq(TourBooking::getBookingNo, bookingNo));
        if (b == null || !b.getUserId().equals(userId)) throw new BusinessException("报名单不存在");
        return b;
    }

    @Override
    public String payForm(Long userId, String bookingNo) {
        TourBooking b = getBooking(userId, bookingNo);
        if (!TourBooking.PENDING.equals(b.getStatus())) throw new BusinessException("报名单不是待支付状态");
        try {
            String subject = "跟团游报名-" + b.getTourTitle() + " x" + b.getPeople() + "人";
            return Factory.Payment.Page()
                    .pay(subject, b.getBookingNo(), b.getTotal().toPlainString(), alipay.getReturnUrl())
                    .body;
        } catch (Exception e) {
            log.error("生成跟团游支付表单失败 bookingNo={}", bookingNo, e);
            throw new BusinessException("支付宝下单失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public TourBooking queryAndSync(Long userId, String bookingNo) {
        TourBooking b = getBooking(userId, bookingNo);
        if (!TourBooking.PENDING.equals(b.getStatus())) return b;
        try {
            AlipayTradeQueryResponse resp = Factory.Payment.Common().query(bookingNo);
            if (!ResponseChecker.success(resp)) return b;
            if ("TRADE_SUCCESS".equals(resp.tradeStatus) || "FINISHED".equals(resp.tradeStatus)) {
                BigDecimal paid = new BigDecimal(resp.totalAmount);
                if (paid.compareTo(b.getTotal()) != 0) {
                    log.error("跟团游支付金额不一致! bookingNo={} 应付={} 实付={}", bookingNo, b.getTotal(), paid);
                    throw new BusinessException("支付金额校验失败，请联系管理员");
                }
                b.setStatus(TourBooking.PAID);
                b.setTradeNo(resp.tradeNo);
                b.setPaidAt(LocalDateTime.now());
                bookingMapper.updateById(b);

                PaymentRecord r = new PaymentRecord();
                r.setOrderNo(bookingNo);
                r.setTradeNo(resp.tradeNo == null ? "" : resp.tradeNo);
                r.setAmount(b.getTotal());
                r.setStatus(resp.tradeStatus);
                paymentMapper.insert(r);
            } else if ("TRADE_CLOSED".equals(resp.tradeStatus)) {
                b.setStatus(TourBooking.CANCELED);
                bookingMapper.updateById(b);
                restoreStock(b);
            }
            return b;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("跟团游查单异常 bookingNo={}", bookingNo, e);
            throw new BusinessException("查询支付结果失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void cancel(Long userId, String bookingNo) {
        TourBooking b = getBooking(userId, bookingNo);
        if (!TourBooking.PENDING.equals(b.getStatus())) throw new BusinessException("当前状态不可取消");
        b.setStatus(TourBooking.CANCELED);
        bookingMapper.updateById(b);
        restoreStock(b);
    }

    private void restoreStock(TourBooking b) {
        LambdaUpdateWrapper<Tour> uw = new LambdaUpdateWrapper<>();
        uw.eq(Tour::getId, b.getTourId()).setSql("stock = stock + " + b.getPeople());
        tourMapper.update(null, uw);
    }

    @Override
    public Map<String, String> contact() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("phone", PHONE);
        m.put("wechat", WECHAT);
        return m;
    }
}
