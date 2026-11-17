package com.gxtourism.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gxtourism.common.BusinessException;
import com.gxtourism.common.Result;
import com.gxtourism.entity.*;
import com.gxtourism.mapper.*;
import com.gxtourism.util.Md5Util;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/** 游客个人中心：资料、密码、收藏/想去、我的投诉、浏览足迹、订单统计 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserCenterController {

    private final UserMapper userMapper;
    private final FavoriteMapper favoriteMapper;
    private final ComplaintMapper complaintMapper;
    private final BrowseLogMapper browseMapper;
    private final JdbcTemplate jdbc;

    // ================= 个人信息 =================

    @GetMapping("/profile")
    public Result<Map<String, Object>> profile(HttpServletRequest req) {
        Long uid = uid(req);
        User u = userMapper.selectById(uid);
        if (u == null) throw new BusinessException("用户不存在");
        u.setPassword(null);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("user", u);
        m.put("orderCount", count("SELECT COUNT(*) FROM t_order WHERE user_id = ?", uid));
        m.put("paidOrders", count("SELECT COUNT(*) FROM t_order WHERE user_id = ? AND status = 'PAID'", uid));
        m.put("totalSpent", jdbc.queryForObject(
                "SELECT COALESCE(SUM(total),0) FROM t_order WHERE user_id = ? AND status='PAID'",
                java.math.BigDecimal.class, uid));
        m.put("favoriteCount", favoriteMapper.selectCount(
                new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, uid)));
        m.put("browseCount", browseMapper.selectCount(
                new LambdaQueryWrapper<BrowseLog>().eq(BrowseLog::getUserId, uid)));
        m.put("complaintCount", complaintMapper.selectCount(
                new LambdaQueryWrapper<Complaint>().eq(Complaint::getUserId, uid)));
        return Result.ok(m);
    }

    @PutMapping("/profile")
    public Result<User> updateProfile(@RequestBody ProfileBody body, HttpServletRequest req) {
        Long uid = uid(req);
        User u = userMapper.selectById(uid);
        if (u == null) throw new BusinessException("用户不存在");
        if (body == null) throw new BusinessException("参数不能为空");
        if (body.getNickname() != null && !body.getNickname().isBlank()) u.setNickname(body.getNickname().trim());
        if (body.getAvatar() != null) u.setAvatar(body.getAvatar());
        if (body.getPhone() != null) u.setPhone(body.getPhone().trim());
        if (body.getEmail() != null) u.setEmail(body.getEmail().trim());
        if ("MERCHANT".equals(u.getRole()) && body.getMerchantName() != null) {
            u.setMerchantName(body.getMerchantName().trim());
        }
        userMapper.updateById(u);
        u.setPassword(null);
        return Result.ok(u);
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody PasswordBody body, HttpServletRequest req) {
        User u = userMapper.selectById(uid(req));
        if (u == null) throw new BusinessException("用户不存在");
        if (body == null || body.getOldPassword() == null || body.getNewPassword() == null) {
            throw new BusinessException("请填写原密码与新密码");
        }
        if (!Md5Util.md5(body.getOldPassword()).equals(u.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        if (body.getNewPassword().length() < 6) throw new BusinessException("新密码至少 6 位");
        u.setPassword(Md5Util.md5(body.getNewPassword()));
        userMapper.updateById(u);
        return Result.ok();
    }

    // ================= 收藏 / 想去 =================

    @GetMapping("/favorites")
    public Result<List<Map<String, Object>>> favorites(HttpServletRequest req) {
        Long uid = uid(req);
        List<Favorite> favs = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, uid).orderByDesc(Favorite::getId));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Favorite f : favs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", f.getId());
            m.put("itemType", f.getItemType());
            m.put("itemId", f.getItemId());
            m.put("createdAt", f.getCreatedAt());
            String title = "", image = "", city = "", price = "0";
            if (Favorite.SPOT.equals(f.getItemType())) {
                List<Map<String, Object>> rows = jdbc.queryForList("SELECT name,cover_img,city,price FROM scenic_spot WHERE id=?", f.getItemId());
                if (!rows.isEmpty()) {
                    title = str(rows.get(0).get("name")); image = str(rows.get(0).get("cover_img"));
                    city = str(rows.get(0).get("city")); price = str(rows.get(0).get("price"));
                }
            } else if (Favorite.PRODUCT.equals(f.getItemType())) {
                List<Map<String, Object>> rows = jdbc.queryForList("SELECT name,cover_img,price,type FROM product WHERE id=?", f.getItemId());
                if (!rows.isEmpty()) {
                    title = str(rows.get(0).get("name")); image = str(rows.get(0).get("cover_img"));
                    price = str(rows.get(0).get("price"));
                }
            } else if (Favorite.TOUR.equals(f.getItemType())) {
                List<Map<String, Object>> rows = jdbc.queryForList("SELECT title,cover_img,city,price FROM tour WHERE id=?", f.getItemId());
                if (!rows.isEmpty()) {
                    title = str(rows.get(0).get("title")); image = str(rows.get(0).get("cover_img"));
                    city = str(rows.get(0).get("city")); price = str(rows.get(0).get("price"));
                }
            } else {
                List<Map<String, Object>> rows = jdbc.queryForList("SELECT name,cover_img FROM shop WHERE id=?", f.getItemId());
                if (!rows.isEmpty()) { title = str(rows.get(0).get("name")); image = str(rows.get(0).get("cover_img")); }
            }
            m.put("title", title); m.put("image", image); m.put("city", city); m.put("price", price);
            m.put("link", link(f.getItemType(), f.getItemId()));
            out.add(m);
        }
        return Result.ok(out);
    }

    /** 收藏 / 取消收藏（幂等切换），返回当前是否已收藏 */
    @PostMapping("/favorites/toggle")
    public Result<Map<String, Object>> toggleFavorite(@RequestBody FavoriteBody body, HttpServletRequest req) {
        Long uid = uid(req);
        if (body == null || body.getItemType() == null || body.getItemId() == null) {
            throw new BusinessException("缺少收藏目标");
        }
        if (!List.of(Favorite.SPOT, Favorite.PRODUCT, Favorite.TOUR, Favorite.SHOP).contains(body.getItemType())) {
            throw new BusinessException("收藏类型不正确");
        }
        List<Favorite> exist = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, uid)
                .eq(Favorite::getItemType, body.getItemType())
                .eq(Favorite::getItemId, body.getItemId()));
        boolean fav;
        if (exist.isEmpty()) {
            Favorite f = new Favorite();
            f.setUserId(uid); f.setItemType(body.getItemType()); f.setItemId(body.getItemId());
            favoriteMapper.insert(f);
            fav = true;
        } else {
            favoriteMapper.deleteById(exist.get(0).getId());
            fav = false;
        }
        return Result.ok(Map.of("favorited", fav));
    }

    @GetMapping("/favorites/check")
    public Result<Boolean> checkFavorite(@RequestParam String itemType, @RequestParam Long itemId,
                                         HttpServletRequest req) {
        Long n = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, uid(req))
                .eq(Favorite::getItemType, itemType)
                .eq(Favorite::getItemId, itemId));
        return Result.ok(n > 0);
    }

    // ================= 我的投诉 =================

    @GetMapping("/complaints")
    public Result<List<Complaint>> myComplaints(HttpServletRequest req) {
        return Result.ok(complaintMapper.selectList(new LambdaQueryWrapper<Complaint>()
                .eq(Complaint::getUserId, uid(req)).orderByDesc(Complaint::getId)));
    }

    @PostMapping("/complaints")
    public Result<Complaint> addComplaint(@RequestBody ComplaintBody body, HttpServletRequest req) {
        Long uid = uid(req);
        User u = userMapper.selectById(uid);
        if (body == null || body.getContent() == null || body.getContent().isBlank()) {
            throw new BusinessException("请填写投诉内容");
        }
        if (body.getContent().length() > 1000) throw new BusinessException("内容不能超过 1000 字");
        Complaint c = new Complaint();
        c.setUserId(uid);
        c.setUserName(u == null ? "" : u.getNickname());
        c.setOrderNo(body.getOrderNo() == null ? "" : body.getOrderNo());
        c.setTargetType(body.getTargetType() == null ? "PLATFORM" : body.getTargetType());
        c.setTargetName(body.getTargetName() == null ? "" : body.getTargetName());
        c.setCategory(body.getCategory() == null ? "其他" : body.getCategory());
        c.setContent(body.getContent().trim());
        c.setStatus(Complaint.PENDING);
        complaintMapper.insert(c);
        return Result.ok(c);
    }

    // ================= 浏览足迹 =================

    @PostMapping("/browse")
    public Result<Void> browse(@RequestBody FavoriteBody body, HttpServletRequest req) {
        Long uid = uid(req);
        if (body == null || body.getItemType() == null || body.getItemId() == null) return Result.ok();
        BrowseLog b = new BrowseLog();
        b.setUserId(uid); b.setItemType(body.getItemType()); b.setItemId(body.getItemId());
        b.setCity(body.getCity() == null ? "" : body.getCity());
        browseMapper.insert(b);
        return Result.ok();
    }

    @GetMapping("/browse")
    public Result<List<Map<String, Object>>> myBrowse(HttpServletRequest req) {
        String sql = """
            SELECT b.id, b.item_type, b.item_id, b.city, b.created_at,
                   COALESCE(s.name, p.name, t.title, sh.name, '已下架内容') AS title,
                   COALESCE(s.cover_img, p.cover_img, t.cover_img, sh.cover_img, '') AS image
              FROM t_browse_log b
              LEFT JOIN scenic_spot s ON b.item_type='SPOT' AND s.id=b.item_id
              LEFT JOIN product p     ON b.item_type='PRODUCT' AND p.id=b.item_id
              LEFT JOIN tour t        ON b.item_type='TOUR' AND t.id=b.item_id
              LEFT JOIN shop sh       ON b.item_type='SHOP' AND sh.id=b.item_id
             WHERE b.user_id = ? ORDER BY b.id DESC LIMIT 30
            """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql, uid(req));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.get("id")); m.put("itemType", r.get("item_type"));
            m.put("itemId", r.get("item_id")); m.put("city", r.get("city"));
            m.put("title", r.get("title")); m.put("image", r.get("image"));
            m.put("createdAt", r.get("created_at"));
            m.put("link", link(str(r.get("item_type")), ((Number) r.get("item_id")).longValue()));
            out.add(m);
        }
        return Result.ok(out);
    }

    // ================= 私有工具 =================

    private Long uid(HttpServletRequest req) { return (Long) req.getAttribute("uid"); }

    private Long count(String sql, Object... args) {
        Long n = jdbc.queryForObject(sql, Long.class, args);
        return n == null ? 0L : n;
    }

    private String str(Object o) { return o == null ? "" : String.valueOf(o); }

    private String link(String type, Long id) {
        return switch (type) {
            case "SPOT" -> "/spots/" + id;
            case "PRODUCT" -> "/spots";
            case "TOUR" -> "/tours";
            case "SHOP" -> "/shops/" + id;
            default -> "/";
        };
    }

    @Data
    public static class ProfileBody {
        private String nickname; private String avatar; private String phone;
        private String email; private String merchantName;
    }
    @Data
    public static class PasswordBody { private String oldPassword; private String newPassword; }
    @Data
    public static class FavoriteBody { private String itemType; private Long itemId; private String city; }
    @Data
    public static class ComplaintBody {
        private String orderNo; private String targetType; private String targetName;
        private String category; private String content;
    }
}
