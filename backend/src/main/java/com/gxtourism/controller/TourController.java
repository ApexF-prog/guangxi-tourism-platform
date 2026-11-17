package com.gxtourism.controller;

import com.gxtourism.common.Result;
import com.gxtourism.entity.Tour;
import com.gxtourism.entity.TourBooking;
import com.gxtourism.service.TourService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tour")
@RequiredArgsConstructor
public class TourController {

    private final TourService tourService;

    /** 线路列表（可按城市筛选，city=全部 或空 为全部） */
    @GetMapping("/list")
    public Result<List<Tour>> list(@RequestParam(required = false) String city) {
        return Result.ok(tourService.list(city));
    }

    /** 线路详情 */
    @GetMapping("/{id}")
    public Result<Tour> detail(@PathVariable Long id) {
        return Result.ok(tourService.detail(id));
    }

    /** 报名（需登录，会员 9 折） */
    @PostMapping("/book")
    public Result<TourBooking> book(@RequestBody BookDTO dto, HttpServletRequest req) {
        return Result.ok(tourService.book((Long) req.getAttribute("uid"),
                dto.getTourId(), dto.getTravelDate(), dto.getPeople(), dto.getContactPhone()));
    }

    /** 我的报名 */
    @GetMapping("/my")
    public Result<List<TourBooking>> my(HttpServletRequest req) {
        return Result.ok(tourService.my((Long) req.getAttribute("uid")));
    }

    /** 生成支付表单 */
    @PostMapping("/pay")
    public Result<String> pay(@RequestBody PayDTO dto, HttpServletRequest req) {
        return Result.ok(tourService.payForm((Long) req.getAttribute("uid"), dto.getBookingNo()));
    }

    /** 支付结果查询（轮询） */
    @GetMapping("/query/{bookingNo}")
    public Result<TourBooking> query(@PathVariable String bookingNo, HttpServletRequest req) {
        return Result.ok(tourService.queryAndSync((Long) req.getAttribute("uid"), bookingNo));
    }

    /** 取消未支付报名 */
    @PostMapping("/cancel/{bookingNo}")
    public Result<Void> cancel(@PathVariable String bookingNo, HttpServletRequest req) {
        tourService.cancel((Long) req.getAttribute("uid"), bookingNo);
        return Result.ok(null);
    }

    /** 咨询联系方式（公开） */
    @GetMapping("/contact")
    public Result<Map<String, String>> contact() {
        return Result.ok(tourService.contact());
    }

    @Data
    public static class BookDTO {
        private Long tourId;
        private String travelDate;
        private Integer people;
        private String contactPhone;
    }

    @Data
    public static class PayDTO {
        private String bookingNo;
    }
}
