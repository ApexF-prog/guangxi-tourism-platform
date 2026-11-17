package com.gxtourism.controller;

import com.gxtourism.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.*;

/**
 * 商家「我的收入」看板：只统计当前登录商家名下的订单，
 * 与其它商家、平台游客数据完全隔离（RoleInterceptor 已挡住非商家账号）。
 */
@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
public class MerchantIncomeController {

    private final JdbcTemplate jdbc;

    @GetMapping("/income")
    public Result<Map<String, Object>> income(HttpServletRequest req) {
        Long mid = (Long) req.getAttribute("uid");

        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Object> summary = jdbc.queryForMap("""
            SELECT COUNT(*) AS paidOrders,
                   COALESCE(SUM(total),0) AS totalAmount,
                   COALESCE(SUM(CASE WHEN DATE(created_at)=CURDATE() THEN total ELSE 0 END),0) AS todayAmount,
                   COALESCE(SUM(CASE WHEN DATE(created_at)=CURDATE() THEN 1 ELSE 0 END),0) AS todayOrders,
                   COALESCE(AVG(total),0) AS avgAmount
              FROM t_order WHERE merchant_id=? AND status='PAID'
            """, mid);
        out.put("summary", camel(summary));

        // 近 7 日趋势：缺失日期补 0，保证折线图连续
        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT DATE(created_at) AS d, COUNT(*) AS cnt, SUM(total) AS amount
              FROM t_order
             WHERE merchant_id=? AND status='PAID'
               AND created_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
             GROUP BY DATE(created_at) ORDER BY d
            """, mid);
        Map<String, Map<String, Object>> byDay = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) {
            byDay.put(String.valueOf(r.get("d")), r);
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            String day = LocalDate.now().minusDays(i).toString();
            Map<String, Object> r = byDay.get(day);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", day);
            m.put("label", day.substring(5));
            m.put("orders", r == null ? 0 : r.get("cnt"));
            m.put("amount", r == null ? 0 : r.get("amount"));
            trend.add(m);
        }
        out.put("trend", trend);

        // 按店铺拆分营业额
        out.put("byShop", camel(jdbc.queryForList("""
            SELECT s.id AS shop_id, s.name AS shop_name,
                   COUNT(o.id) AS order_count, COALESCE(SUM(o.total),0) AS amount
              FROM shop s LEFT JOIN t_order o
                ON o.shop_id=s.id AND o.status='PAID'
             WHERE s.merchant_id=?
             GROUP BY s.id, s.name ORDER BY amount DESC
            """, mid)));

        // 商品收入贡献 Top 8
        out.put("byProduct", camel(jdbc.queryForList("""
            SELECT o.product_id, o.product_name,
                   SUM(o.quantity) AS qty, SUM(o.total) AS amount
              FROM t_order o
             WHERE o.merchant_id=? AND o.status='PAID'
             GROUP BY o.product_id, o.product_name
             ORDER BY amount DESC LIMIT 8
            """, mid)));

        return Result.ok(out);
    }

    private List<Map<String, Object>> camel(List<Map<String, Object>> rows) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : row.entrySet()) {
                StringBuilder k = new StringBuilder();
                boolean upper = false;
                for (char ch : e.getKey().toCharArray()) {
                    if (ch == '_') upper = true;
                    else { k.append(upper ? Character.toUpperCase(ch) : ch); upper = false; }
                }
                m.put(k.toString(), e.getValue());
            }
            out.add(m);
        }
        return out;
    }

    private Map<String, Object> camel(Map<String, Object> row) {
        return camel(List.of(row)).get(0);
    }
}
