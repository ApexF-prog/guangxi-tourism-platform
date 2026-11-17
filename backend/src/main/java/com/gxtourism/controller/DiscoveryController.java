package com.gxtourism.controller;

import com.gxtourism.common.Result;
import com.gxtourism.entity.Favorite;
import com.gxtourism.mapper.FavoriteMapper;
import com.gxtourism.util.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 公开发现接口：热搜联想、实时订单播报、平台配置、个性化推荐。
 *
 * 推荐采用「用户行为画像 + 热度」的加权打分（轻量协同过滤思路）：
 * 浏览足迹 / 收藏 → 城市与类型偏好 → 候选打分 → 返回带推荐理由的榜单；
 * 未登录或无行为时自动降级为全局热度榜，保证首页永不空白。
 */
@RestController
@RequestMapping("/api/discovery")
@RequiredArgsConstructor
public class DiscoveryController {

    private final JdbcTemplate jdbc;
    private final JwtUtil jwtUtil;
    private final FavoriteMapper favoriteMapper;

    private static final List<String> FALLBACK_HOT = List.of(
            "桂林山水", "漓江竹筏", "龙脊梯田", "北海银滩", "青秀山", "柳州螺蛳粉", "德天瀑布", "遇龙河");

    /** 热搜榜：来自真实浏览足迹，不足时补默认热词 */
    @GetMapping("/hot-search")
    public Result<List<Map<String, Object>>> hotSearch() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT s.name AS word, COUNT(*) AS cnt, s.city AS city
              FROM t_browse_log b JOIN scenic_spot s ON b.item_type='SPOT' AND b.item_id=s.id
             GROUP BY s.id, s.name, s.city ORDER BY cnt DESC, s.id ASC LIMIT 8
            """);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("word", r.get("word")); m.put("count", r.get("cnt")); m.put("city", r.get("city"));
            out.add(m);
        }
        for (String w : FALLBACK_HOT) {
            if (out.size() >= 8) break;
            boolean exists = out.stream().anyMatch(m -> w.equals(m.get("word")));
            if (!exists) out.add(Map.of("word", w, "count", 0, "city", ""));
        }
        return Result.ok(out);
    }

    /** 实时订单播报：最近已支付订单，昵称脱敏 */
    @GetMapping("/live-feed")
    public Result<List<Map<String, Object>>> liveFeed() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
            SELECT o.product_name, o.total, u.nickname, o.created_at, o.spot_name
              FROM t_order o LEFT JOIN t_user u ON o.user_id=u.id
             WHERE o.status='PAID' ORDER BY o.id DESC LIMIT 8
            """);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("productName", str(r.get("product_name")));
            m.put("amount", str(r.get("total")));
            m.put("buyer", mask(str(r.get("nickname"))));
            m.put("spotName", str(r.get("spot_name")));
            m.put("createdAt", r.get("created_at"));
            out.add(m);
        }
        if (out.isEmpty()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("productName", "桂林山水三日游"); m.put("amount", "688.00");
            m.put("buyer", "13****89"); m.put("spotName", "桂林"); m.put("createdAt", null);
            out.add(m);
        }
        return Result.ok(out);
    }

    /** 公开平台配置：公告、客服电话、会员折扣等 */
    @GetMapping("/config")
    public Result<Map<String, String>> publicConfig() {
        Map<String, String> out = new LinkedHashMap<>();
        for (Map<String, Object> r : jdbc.queryForList(
                "SELECT config_key, config_value FROM sys_config")) {
            out.put(str(r.get("config_key")), str(r.get("config_value")));
        }
        out.putIfAbsent("platform_name", "广西旅游网");
        out.putIfAbsent("service_phone", "19175657645");
        return Result.ok(out);
    }

    /** 搜索联想：景点 / 商品 / 线路名称模糊匹配 */
    @GetMapping("/suggest")
    public Result<List<String>> suggest(@RequestParam(required = false) String q) {
        if (q == null || q.isBlank()) return Result.ok(List.of());
        String kw = "%" + q.trim() + "%";
        List<String> out = new ArrayList<>();
        out.addAll(jdbc.queryForList(
                "SELECT name FROM scenic_spot WHERE name LIKE ? ORDER BY hot DESC LIMIT 5", String.class, kw));
        if (out.size() < 8) out.addAll(jdbc.queryForList(
                "SELECT name FROM product WHERE name LIKE ? AND status=1 ORDER BY sales DESC LIMIT 5", String.class, kw));
        if (out.size() < 8) out.addAll(jdbc.queryForList(
                "SELECT title FROM tour WHERE title LIKE ? AND status=1 LIMIT 4", String.class, kw));
        return Result.ok(out.stream().distinct().limit(8).toList());
    }

    /** 个性化推荐：登录后按行为画像打分，未登录降级热度榜 */
    @GetMapping("/recommend")
    public Result<Map<String, Object>> recommend(@RequestParam(defaultValue = "8") int limit,
                                                 HttpServletRequest req) {
        Long uid = optionalUid(req);
        limit = Math.max(4, Math.min(limit, 16));

        String prefCity = "";
        Set<Long> favIds = new HashSet<>();
        Set<String> favTypes = new HashSet<>();
        if (uid != null) {
            List<Map<String, Object>> cityRows = jdbc.queryForList("""
                SELECT city, COUNT(*) AS cnt FROM t_browse_log
                 WHERE user_id=? AND city<>'' GROUP BY city ORDER BY cnt DESC LIMIT 1
                """, uid);
            if (!cityRows.isEmpty()) prefCity = str(cityRows.get(0).get("city"));
            if (prefCity.isEmpty()) {
                cityRows = jdbc.queryForList("""
                    SELECT s.city AS city, COUNT(*) AS cnt FROM favorite f
                      JOIN scenic_spot s ON f.item_type='SPOT' AND f.item_id=s.id
                     WHERE f.user_id=? GROUP BY s.city ORDER BY cnt DESC LIMIT 1
                    """, uid);
                if (!cityRows.isEmpty()) prefCity = str(cityRows.get(0).get("city"));
            }
            for (Favorite f : favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                    .eq(Favorite::getUserId, uid))) {
                favIds.add(f.getItemId());
                favTypes.add(f.getItemType());
            }
        }

        // 候选集：在售商品 + 景点 + 上架线路，一次性捞回后在内存打分（数据量小，避免复杂 SQL）
        List<Candidate> pool = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                SELECT id,name,city,cover_img,price,hot FROM scenic_spot
                """)) {
            pool.add(new Candidate("SPOT", num(r.get("id")), str(r.get("name")), str(r.get("city")),
                    str(r.get("cover_img")), str(r.get("price")), (double) num(r.get("hot")), "/spots/" + r.get("id")));
        }
        for (Map<String, Object> r : jdbc.queryForList("""
                SELECT p.id,p.name,p.cover_img,p.price,p.sales,s.city
                  FROM product p LEFT JOIN scenic_spot s ON p.spot_id=s.id
                 WHERE p.status=1
                """)) {
            pool.add(new Candidate("PRODUCT", num(r.get("id")), str(r.get("name")), str(r.get("city")),
                    str(r.get("cover_img")), str(r.get("price")), (double) num(r.get("sales")), "/spots"));
        }
        for (Map<String, Object> r : jdbc.queryForList("""
                SELECT id,title,city,cover_img,price FROM tour WHERE status=1
                """)) {
            pool.add(new Candidate("TOUR", num(r.get("id")), str(r.get("title")), str(r.get("city")),
                    str(r.get("cover_img")), str(r.get("price")), 30.0, "/tours"));
        }

        double maxHeat = pool.stream().mapToDouble(c -> c.heat).max().orElse(1.0);
        if (maxHeat <= 0) maxHeat = 1.0;
        boolean personalized = !prefCity.isEmpty();
        String finalCity = prefCity;
        Set<Long> finalFavIds = favIds;
        Set<String> finalFavTypes = favTypes;

        for (Candidate c : pool) {
            double score = 20.0 + (c.heat / maxHeat) * 30.0;
            String reason = "广西热门推荐";
            if (personalized) {
                if (!finalCity.isEmpty() && finalCity.equals(c.city)) { score += 45; reason = "因为你关注" + finalCity; }
                if (finalFavTypes.contains(c.itemType)) { score += 10; reason = "猜你喜欢 · " + c.itemType; }
                if (!finalCity.isEmpty() && finalCity.equals(c.city) && finalFavTypes.contains(c.itemType)) {
                    score += 8; reason = "常逛" + finalCity + "的人都在看";
                }
            }
            if (finalFavIds.contains(c.itemId)) score -= 25;  // 已收藏的降权
            score += (c.itemId * 7 % 11) * 0.3;               // 稳定微扰，避免每次完全一致
            c.score = score;
            c.reason = reason;
        }

        List<Map<String, Object>> items = pool.stream()
                .sorted(Comparator.comparingDouble((Candidate c) -> c.score).reversed())
                .limit(limit)
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("itemType", c.itemType); m.put("itemId", c.itemId);
                    m.put("title", c.title); m.put("city", c.city); m.put("image", c.image);
                    m.put("price", c.price); m.put("reason", c.reason); m.put("link", c.link);
                    m.put("score", Math.round(c.score * 10) / 10.0);
                    return m;
                }).toList();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("personalized", personalized);
        out.put("prefCity", prefCity);
        out.put("title", personalized ? "为你推荐 · " + prefCity + "及相似目的地" : "大家都在看 · 广西热门榜");
        out.put("items", items);
        return Result.ok(out);
    }

    // ================= 私有工具 =================

    private Long optionalUid(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            try { return jwtUtil.verify(auth.substring(7)); } catch (Exception ignored) { }
        }
        return null;
    }

    private String str(Object o) { return o == null ? "" : String.valueOf(o); }

    private long num(Object o) {
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(o)); } catch (Exception e) { return 0L; }
    }

    private String mask(String name) {
        if (name == null || name.isBlank()) return "游客";
        if (name.length() <= 2) return name.charAt(0) + "*";
        return name.charAt(0) + "****" + name.charAt(name.length() - 1);
    }

    private static class Candidate {
        final String itemType; final long itemId; final String title; final String city;
        final String image; final String price; final double heat; final String link;
        double score; String reason;
        Candidate(String t, long id, String title, String city, String image, String price, double heat, String link) {
            this.itemType = t; this.itemId = id; this.title = title; this.city = city;
            this.image = image; this.price = price; this.heat = heat; this.link = link;
        }
    }
}
