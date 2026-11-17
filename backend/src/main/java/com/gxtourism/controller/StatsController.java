package com.gxtourism.controller;

import com.gxtourism.common.Result;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 可视化大屏统计接口（公开，无需登录）。
 *
 * 数据口径：
 * - kpiList / priceCompare / typeShare / scenicSpots / liveOrders：
 *   全部由 t_order、product、scenic_spot 等真实业务表 SQL 聚合，下单后立即变化；
 * - cityHeat / monthlyTrend / sourceStructure：
 *   来自 t_visit_stat 客流事实表（历史 24 个月为演示种子数据 SEED），
 *   当月会叠加真实订单数量，属于「行业客流统计」口径。
 *
 * 前端在接口不可用时会退回本地基线数据，保证大屏永不空白。
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final JdbcTemplate jdbc;

    public StatsController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static long num(Object v) {
        return v == null ? 0L : ((Number) v).longValue();
    }

    private static BigDecimal money(Object v) {
        if (v == null) return BigDecimal.ZERO;
        return v instanceof BigDecimal ? (BigDecimal) v : new BigDecimal(v.toString());
    }

    private static Map<String, Object> kpi(String label, Object value, String unit, String tone) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label);
        m.put("value", value);
        m.put("unit", unit);
        m.put("tone", tone);
        return m;
    }

    @GetMapping("/screen")
    public Result<Map<String, Object>> screen() {
        Map<String, Object> out = new LinkedHashMap<>();

        // ---------- 1. 顶部 KPI：全部是数据库真实业务指标 ----------
        Map<String, Object> biz = jdbc.queryForMap(
                "SELECT COUNT(*) AS orders, " +
                "COALESCE(SUM(CASE WHEN status='PAID' THEN total ELSE 0 END),0) AS revenue, " +
                "SUM(CASE WHEN DATE(created_at)=CURDATE() THEN 1 ELSE 0 END) AS today_orders " +
                "FROM t_order");
        long orders = num(biz.get("orders"));
        BigDecimal revenue = money(biz.get("revenue"));
        long todayOrders = num(biz.get("today_orders"));
        long spotCount = num(jdbc.queryForObject("SELECT COUNT(*) FROM scenic_spot", Object.class));
        long productCount = num(jdbc.queryForObject("SELECT COUNT(*) FROM product WHERE status=1", Object.class));

        List<Map<String, Object>> kpiList = new ArrayList<>();
        kpiList.add(kpi("平台累计订单", orders, "笔", "cyan"));
        kpiList.add(kpi("平台交易额", revenue.setScale(1, java.math.RoundingMode.HALF_UP), "元", "amber"));
        kpiList.add(kpi("在监测景区", spotCount, "个", "teal"));
        kpiList.add(kpi("在售商品", productCount, "件", "amber"));
        out.put("kpiList", kpiList);

        // ---------- 2. 门票价格对比：真实 scenic_spot ----------
        List<Map<String, Object>> priceCompare = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList(
                "SELECT name, city, price FROM scenic_spot WHERE price > 0 ORDER BY price DESC LIMIT 10")) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", r.get("name"));
            m.put("city", r.get("city"));
            m.put("value", money(r.get("price")).doubleValue());
            priceCompare.add(m);
        }
        out.put("priceCompare", priceCompare);

        // ---------- 3. 业态构成：真实 product.type ----------
        Map<String, String> typeNames = Map.of(
                "TICKET", "景区门票", "FOOD", "特色美食",
                "DRINK", "茶饮饮品", "PACKAGE", "团购套餐");
        List<Map<String, Object>> typeShare = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList(
                "SELECT type, COUNT(*) AS c FROM product WHERE status=1 GROUP BY type ORDER BY c DESC")) {
            Map<String, Object> m = new LinkedHashMap<>();
            String t = String.valueOf(r.get("type"));
            m.put("name", typeNames.getOrDefault(t, t));
            m.put("value", num(r.get("c")));
            typeShare.add(m);
        }
        out.put("typeShare", typeShare);

        // ---------- 4. 地图散点：真实景点 + 前端静态经纬度字典 ----------
        List<Map<String, Object>> scenicSpots = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList(
                "SELECT name, city, level, price, hot FROM scenic_spot ORDER BY hot DESC, id ASC LIMIT 18")) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", r.get("name"));
            m.put("city", r.get("city"));
            m.put("star", r.get("level"));
            m.put("price", money(r.get("price")).doubleValue());
            m.put("heat", Math.max(58, Math.min(96, 58 + num(r.get("hot")) / 20)));
            scenicSpots.add(m);
        }
        out.put("scenicSpots", scenicSpots);

        // ---------- 5. 城市热度 / 客源结构：t_visit_stat 当年合计 ----------
        int thisYear = LocalDateTime.now().getYear();
        int seedYear = thisYear > 2026 ? 2026 : thisYear;
        int prevYear = seedYear - 1;

        List<Map<String, Object>> cityHeat = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList(
                "SELECT city, SUM(inside_visitors + outside_visitors) AS v, " +
                "MAX(spot_count) AS s FROM t_visit_stat " +
                "WHERE stat_year = ? AND data_source='SEED' GROUP BY city ORDER BY v DESC", seedYear)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", r.get("city"));
            m.put("value", num(r.get("v")));
            m.put("spots", num(r.get("s")));
            cityHeat.add(m);
        }
        out.put("cityHeat", cityHeat);

        List<Map<String, Object>> sourceStructure = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList(
                "SELECT city, SUM(inside_visitors) AS inside, SUM(outside_visitors) AS outside " +
                "FROM t_visit_stat WHERE stat_year = ? AND data_source='SEED' " +
                "GROUP BY city ORDER BY inside + outside DESC LIMIT 7", seedYear)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("city", String.valueOf(r.get("city")).replace("市", ""));
            m.put("inside", num(r.get("inside")));
            m.put("outside", num(r.get("outside")));
            sourceStructure.add(m);
        }
        out.put("sourceStructure", sourceStructure);

        // ---------- 6. 月度趋势：事实表按月聚合，当月叠加真实订单 ----------
        List<String> months = new ArrayList<>();
        for (int i = 1; i <= 12; i++) months.add(i + "月");
        out.put("months", months);

        Map<String, List<Long>> monthlyTrend = new LinkedHashMap<>();
        for (int y : new int[]{seedYear, prevYear}) {
            Map<Integer, Long> byMonth = new LinkedHashMap<>();
            for (int i = 1; i <= 12; i++) byMonth.put(i, 0L);
            for (Map<String, Object> r : jdbc.queryForList(
                    "SELECT stat_month AS m, SUM(inside_visitors + outside_visitors) AS v " +
                    "FROM t_visit_stat WHERE stat_year = ? AND data_source='SEED' GROUP BY stat_month", y)) {
                byMonth.put(((Number) r.get("m")).intValue(), num(r.get("v")));
            }
            List<Long> series = new ArrayList<>();
            for (int i = 1; i <= 12; i++) series.add(byMonth.get(i));
            monthlyTrend.put(y + "年", series);
        }
        // 当月真实订单量（笔）折算成客流人次叠加，体现实时链路
        int monthNow = LocalDateTime.now().getMonthValue();
        long liveOrdersThisMonth = num(jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_order WHERE YEAR(created_at)=? AND MONTH(created_at)=?",
                Object.class, thisYear, monthNow));
        List<Long> cur = monthlyTrend.get(seedYear + "年");
        cur.set(monthNow - 1, cur.get(monthNow - 1) + liveOrdersThisMonth);
        out.put("monthlyTrend", monthlyTrend);

        // ---------- 7. 元信息 ----------
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("orderCount", orders);
        meta.put("todayOrders", todayOrders);
        meta.put("revenue", revenue);
        meta.put("liveOrdersThisMonth", liveOrdersThisMonth);
        meta.put("updatedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        meta.put("seedYear", seedYear);
        out.put("meta", meta);

        return Result.ok(out);
    }
}
