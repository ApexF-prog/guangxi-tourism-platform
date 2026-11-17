package com.gxtourism.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gxtourism.common.BusinessException;
import com.gxtourism.common.Result;
import com.gxtourism.entity.*;
import com.gxtourism.mapper.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 平台总后台 · 运营能力：商家审核、游客与商家全景、商品上下架、
 * 平台配置、投诉工单、操作审计、CSV 导出。
 *
 * 与 AdminController 共用 /api/admin 前缀，但路径互不重叠；
 * 每个方法都先 requireAdmin，前端隐藏菜单只是体验，真正的门禁在这里。
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminPlatformController {

    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final ComplaintMapper complaintMapper;
    private final SysConfigMapper configMapper;
    private final AdminLogMapper logMapper;
    private final JdbcTemplate jdbc;

    // ================= 商家全景 + 入驻审核 =================

    /** 全部入驻商家：含店铺数 / 商品数 / 订单数 / 营业额 / 审核状态 */
    @GetMapping("/merchants")
    public Result<List<Map<String, Object>>> merchants(HttpServletRequest req) {
        requireAdmin(req);
        String sql = """
            SELECT u.id, u.username, u.nickname, u.merchant_name, u.phone, u.email,
                   u.audit_status, u.audit_remark, u.created_at,
                   (SELECT COUNT(*) FROM shop s WHERE s.merchant_id = u.id) AS shop_count,
                   (SELECT COUNT(*) FROM product p JOIN shop s2 ON p.shop_id = s2.id
                      WHERE s2.merchant_id = u.id) AS product_count,
                   (SELECT COUNT(*) FROM t_order o WHERE o.merchant_id = u.id) AS order_count,
                   (SELECT COALESCE(SUM(o.total), 0) FROM t_order o
                      WHERE o.merchant_id = u.id AND o.status = 'PAID') AS turnover
              FROM t_user u WHERE u.role = 'MERCHANT' ORDER BY u.id
            """;
        return Result.ok(camel(jdbc.queryForList(sql)));
    }

    /** 审核商家：通过 / 驳回；驳回后该账号无法进入商家后台 */
    @PutMapping("/merchants/{id}/audit")
    public Result<User> audit(@PathVariable Long id, @RequestBody AuditBody body, HttpServletRequest req) {
        Long me = requireAdmin(req);
        User u = userMapper.selectById(id);
        if (u == null || !"MERCHANT".equals(u.getRole())) throw new BusinessException("该商家不存在");
        String status = body == null || body.getStatus() == null ? "" : body.getStatus().toUpperCase();
        if (!List.of("APPROVED", "REJECTED", "PENDING").contains(status)) {
            throw new BusinessException("审核状态只能是 APPROVED / REJECTED / PENDING");
        }
        u.setAuditStatus(status);
        u.setAuditRemark(body.getRemark() == null ? "" : body.getRemark());
        userMapper.updateById(u);
        log(req, me, "审核商家", u.getNickname() + " → " + status + "；" + u.getAuditRemark());
        u.setPassword(null);
        return Result.ok(u);
    }

    // ================= 游客全景 =================

    /** 所有注册游客：含订单数、累计消费、是否会员 */
    @GetMapping("/customers")
    public Result<List<Map<String, Object>>> customers(HttpServletRequest req) {
        requireAdmin(req);
        String sql = """
            SELECT u.id, u.username, u.nickname, u.avatar, u.vip_level, u.phone, u.email, u.created_at,
                   (SELECT COUNT(*) FROM t_order o WHERE o.user_id = u.id) AS order_count,
                   (SELECT COALESCE(SUM(o.total), 0) FROM t_order o
                      WHERE o.user_id = u.id AND o.status = 'PAID') AS total_spent
              FROM t_user u WHERE u.role = 'CUSTOMER' ORDER BY u.id
            """;
        return Result.ok(camel(jdbc.queryForList(sql)));
    }

    /** 指定游客的全部订单；不传 userId 则返回全部游客订单 */
    @GetMapping("/customer-orders")
    public Result<List<Map<String, Object>>> customerOrders(@RequestParam(required = false) Long userId,
                                                             HttpServletRequest req) {
        requireAdmin(req);
        List<Object> args = new ArrayList<>();
        String sql = """
            SELECT o.id, o.order_no, o.user_id, u.nickname AS user_name, o.product_name,
                   o.quantity, o.total, o.status, o.pick_no, o.created_at
              FROM t_order o LEFT JOIN t_user u ON o.user_id = u.id
             WHERE u.role = 'CUSTOMER'
            """;
        if (userId != null) { sql += " AND o.user_id = ? "; args.add(userId); }
        sql += " ORDER BY o.created_at DESC LIMIT 500 ";
        return Result.ok(camel(jdbc.queryForList(sql, args.toArray())));
    }

    // ================= 商品全景 + 上下架 =================

    /** 全部商品（可按商家 / 状态 / 关键词筛选），带店铺、商家、景点名称 */
    @GetMapping("/products")
    public Result<List<Map<String, Object>>> products(@RequestParam(required = false) Long merchantId,
                                                      @RequestParam(required = false) Integer status,
                                                      @RequestParam(required = false) String keyword,
                                                      HttpServletRequest req) {
        requireAdmin(req);
        List<Object> args = new ArrayList<>();
        String sql = """
            SELECT p.id, p.name, p.type, p.price, p.original_price, p.stock, p.sales, p.status,
                   p.cover_img, p.shop_id, p.merchant_id,
                   s.name AS shop_name, u.nickname AS merchant_name, sp.name AS spot_name, sp.city AS city
              FROM product p
              LEFT JOIN shop s ON p.shop_id = s.id
              LEFT JOIN t_user u ON s.merchant_id = u.id
              LEFT JOIN scenic_spot sp ON p.spot_id = sp.id
             WHERE 1 = 1
            """;
        if (merchantId != null) { sql += " AND s.merchant_id = ? "; args.add(merchantId); }
        if (status != null) { sql += " AND p.status = ? "; args.add(status); }
        if (keyword != null && !keyword.isBlank()) { sql += " AND p.name LIKE ? "; args.add("%" + keyword.trim() + "%"); }
        sql += " ORDER BY p.id ASC LIMIT 500 ";
        return Result.ok(camel(jdbc.queryForList(sql, args.toArray())));
    }

    /** 上架 / 下架商品：0 下架后前端列表不再展示，1 恢复 */
    @PutMapping("/products/{id}/status")
    public Result<Product> productStatus(@PathVariable Long id, @RequestParam Integer status,
                                          HttpServletRequest req) {
        Long me = requireAdmin(req);
        if (!List.of(0, 1).contains(status)) throw new BusinessException("状态只能是 0(下架) 或 1(上架)");
        Product p = productMapper.selectById(id);
        if (p == null) throw new BusinessException("商品不存在");
        p.setStatus(status);
        productMapper.updateById(p);
        log(req, me, status == 1 ? "上架商品" : "下架商品", p.getName() + "（#" + id + "）");
        return Result.ok(p);
    }

    /** 商家订单：不传 merchantId 返回全部商家订单 */
    @GetMapping("/merchant-orders")
    public Result<List<Map<String, Object>>> merchantOrders(@RequestParam(required = false) Long merchantId,
                                                             HttpServletRequest req) {
        requireAdmin(req);
        List<Object> args = new ArrayList<>();
        String sql = """
            SELECT o.id, o.order_no, o.merchant_id, u.nickname AS merchant_name,
                   o.product_name, o.quantity, o.total, o.status, o.pick_no, o.created_at
              FROM t_order o LEFT JOIN t_user u ON o.merchant_id = u.id
             WHERE o.merchant_id IS NOT NULL
            """;
        if (merchantId != null) { sql += " AND o.merchant_id = ? "; args.add(merchantId); }
        sql += " ORDER BY o.created_at DESC LIMIT 500 ";
        return Result.ok(camel(jdbc.queryForList(sql, args.toArray())));
    }

    // ================= 平台配置 =================

    @GetMapping("/configs")
    public Result<List<SysConfig>> configs(HttpServletRequest req) {
        requireAdmin(req);
        return Result.ok(configMapper.selectList(new LambdaQueryWrapper<SysConfig>()
                .orderByAsc(SysConfig::getConfigKey)));
    }

    @PutMapping("/configs/{key}")
    public Result<SysConfig> updateConfig(@PathVariable String key, @RequestBody ConfigBody body,
                                          HttpServletRequest req) {
        Long me = requireAdmin(req);
        SysConfig c = configMapper.selectById(key);
        if (c == null) {
            c = new SysConfig();
            c.setConfigKey(key);
            c.setDescription("管理员新增配置");
            c.setConfigValue(body == null || body.getValue() == null ? "" : body.getValue());
            configMapper.insert(c);
        } else {
            c.setConfigValue(body == null || body.getValue() == null ? "" : body.getValue());
            configMapper.updateById(c);
        }
        log(req, me, "修改平台配置", key + " = " + c.getConfigValue());
        return Result.ok(c);
    }

    // ================= 投诉工单 =================

    @GetMapping("/complaints")
    public Result<List<Complaint>> complaints(@RequestParam(required = false) String status,
                                              HttpServletRequest req) {
        requireAdmin(req);
        return Result.ok(complaintMapper.selectList(new LambdaQueryWrapper<Complaint>()
                .eq(status != null && !status.isBlank(), Complaint::getStatus, status)
                .orderByDesc(Complaint::getCreatedAt)));
    }

    @PutMapping("/complaints/{id}")
    public Result<Complaint> handleComplaint(@PathVariable Long id, @RequestBody HandleBody body,
                                             HttpServletRequest req) {
        Long me = requireAdmin(req);
        Complaint c = complaintMapper.selectById(id);
        if (c == null) throw new BusinessException("工单不存在");
        String status = body == null || body.getStatus() == null ? "" : body.getStatus().toUpperCase();
        if (!List.of("PENDING", "PROCESSING", "RESOLVED", "REJECTED").contains(status)) {
            throw new BusinessException("工单状态不正确");
        }
        c.setStatus(status);
        c.setReply(body.getReply() == null ? "" : body.getReply());
        c.setHandlerId(me);
        c.setHandledAt(LocalDateTime.now());
        complaintMapper.updateById(c);
        log(req, me, "处理投诉", "#" + id + " → " + status);
        return Result.ok(c);
    }

    // ================= 审计日志 & 导出 =================

    @GetMapping("/logs")
    public Result<List<AdminLog>> logs(HttpServletRequest req) {
        requireAdmin(req);
        return Result.ok(logMapper.selectList(new LambdaQueryWrapper<AdminLog>()
                .orderByDesc(AdminLog::getId).last("LIMIT 100")));
    }

    /** 导出 CSV 文本，前端用 Blob 生成下载文件 */
    @GetMapping("/export")
    public Result<String> export(@RequestParam(defaultValue = "orders") String type,
                                 HttpServletRequest req) {
        requireAdmin(req);
        String sql = switch (type) {
            case "customers" -> "SELECT id,username,nickname,role,vip_level,phone,email,created_at FROM t_user";
            case "merchants" -> "SELECT id,username,nickname,merchant_name,audit_status,phone,created_at FROM t_user WHERE role='MERCHANT'";
            case "products"  -> "SELECT id,name,type,price,stock,sales,status,shop_id FROM product";
            case "complaints"-> "SELECT id,user_name,category,target_name,content,status,reply,created_at FROM complaint";
            default          -> "SELECT id,order_no,user_id,product_name,quantity,total,status,pick_no,created_at FROM t_order";
        };
        List<Map<String, Object>> rows = jdbc.queryForList(sql + " ORDER BY id ASC ");
        StringBuilder sb = new StringBuilder();
        if (!rows.isEmpty()) {
            sb.append(String.join(",", rows.get(0).keySet())).append('\n');
            for (Map<String, Object> r : rows) {
                List<String> cells = new ArrayList<>();
                for (Object v : r.values()) {
                    String s = String.valueOf(v == null ? "" : v).replace("\"", "\"\"");
                    cells.add("\"" + s + "\"");
                }
                sb.append(String.join(",", cells)).append('\n');
            }
        }
        return Result.ok(sb.toString());
    }

    // ================= 私有工具 =================

    private Long requireAdmin(HttpServletRequest req) {
        Long id = (Long) req.getAttribute("uid");
        User u = id == null ? null : userMapper.selectById(id);
        if (u == null || !"ADMIN".equals(u.getRole())) throw new BusinessException("仅平台管理员可操作");
        return id;
    }

    private void log(HttpServletRequest req, Long me, String action, String detail) {
        try {
            User admin = userMapper.selectById(me);
            AdminLog l = new AdminLog();
            l.setAdminId(me);
            l.setAdminName(admin == null ? "" : admin.getNickname());
            l.setAction(action);
            l.setDetail(detail == null ? "" : detail);
            logMapper.insert(l);
        } catch (Exception ignored) {
            // 日志失败不应阻断主业务
        }
    }

    /** JdbcTemplate 返回下划线列名，统一转成前端习惯的驼峰 */
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

    @Data
    public static class AuditBody { private String status; private String remark; }
    @Data
    public static class ConfigBody { private String value; }
    @Data
    public static class HandleBody { private String status; private String reply; }
}
