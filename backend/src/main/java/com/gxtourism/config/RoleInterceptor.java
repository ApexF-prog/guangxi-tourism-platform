package com.gxtourism.config;

import com.gxtourism.entity.User;
import com.gxtourism.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 角色门禁：把「谁能进哪个后台」统一收口到服务端，前端隐藏菜单只是体验层。
 *
 * /api/admin/**    → 仅 ADMIN
 * /api/merchant/** → 仅 MERCHANT，且入驻审核必须为 APPROVED
 *
 * 必须在 JwtInterceptor 之后执行（WebConfig 里按注册顺序），此时 request 上已有 uid。
 */
@Component
@RequiredArgsConstructor
public class RoleInterceptor implements HandlerInterceptor {

    private final UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = request.getRequestURI();
        boolean adminPath = path.startsWith("/api/admin");
        boolean merchantPath = path.startsWith("/api/merchant");
        if (!adminPath && !merchantPath) return true;

        Long uid = (Long) request.getAttribute("uid");
        User u = uid == null ? null : userMapper.selectById(uid);
        String want = adminPath ? "ADMIN" : "MERCHANT";
        if (u == null || !want.equals(u.getRole())) {
            return deny(response, 403, "当前账号无权访问该后台，请切换对应角色登录");
        }
        if (merchantPath && !"APPROVED".equals(u.getAuditStatus())
                && !"".equals(u.getAuditStatus()) && u.getAuditStatus() != null) {
            return deny(response, 403, "商家入驻审核尚未通过，请联系平台管理员");
        }
        // 兼容历史数据：audit_status 为空视为通过
        return true;
    }

    private boolean deny(HttpServletResponse response, int code, String msg) throws Exception {
        response.setStatus(code);
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write("{\"code\":" + code + ",\"msg\":\"" + msg + "\"}");
        return false;
    }
}
