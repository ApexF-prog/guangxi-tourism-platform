package com.gxtourism.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final RoleInterceptor roleInterceptor;

    public WebConfig(JwtInterceptor jwtInterceptor, RoleInterceptor roleInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
        this.roleInterceptor = roleInterceptor;
    }

    /** 需要登录的接口：下单、我的订单、发起支付、查支付结果、评价、个人中心、商家/管理后台 */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns(
                        "/api/order/**",
                        "/api/pay/alipay/create",
                        "/api/pay/alipay/cashier",
                        "/api/pay/alipay/query/**",
                        "/api/review",
                        "/api/user/**",
                        "/api/merchant/**",
                        "/api/admin/**",
                        // 会员：套餐页公开，开通/查单需登录
                        "/api/vip/status",
                        "/api/vip/create",
                        "/api/vip/query/**",
                        // 跟团游：浏览公开，报名/支付需登录
                        "/api/tour/book",
                        "/api/tour/my",
                        "/api/tour/pay",
                        "/api/tour/query/**",
                        "/api/tour/cancel/**")
                .excludePathPatterns("/api/review/list");

        // 角色门禁必须排在登录拦截之后：此时 request 上已有 uid
        // /api/discovery/** 为公开接口，不在任何拦截路径内
        registry.addInterceptor(roleInterceptor)
                .addPathPatterns("/api/admin/**", "/api/merchant/**");
    }

    /** 前后端分离跨域：本地 5173 与线上域名都放开 */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
