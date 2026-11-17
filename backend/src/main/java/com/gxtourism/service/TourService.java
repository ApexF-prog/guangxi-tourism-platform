package com.gxtourism.service;

import com.gxtourism.entity.Tour;
import com.gxtourism.entity.TourBooking;

import java.util.List;
import java.util.Map;

public interface TourService {

    List<Tour> list(String city);

    Tour detail(Long id);

    /** 报名（生成待支付报名单，会员 9 折） */
    TourBooking book(Long userId, Long tourId, String travelDate, Integer people, String contactPhone);

    List<TourBooking> my(Long userId);

    TourBooking getBooking(Long userId, String bookingNo);

    /** 生成支付宝沙箱支付表单 */
    String payForm(Long userId, String bookingNo);

    /** 查询支付结果并同步（幂等） */
    TourBooking queryAndSync(Long userId, String bookingNo);

    /** 取消未支付的报名（回补名额） */
    void cancel(Long userId, String bookingNo);

    /** 咨询联系方式 */
    Map<String, String> contact();
}
