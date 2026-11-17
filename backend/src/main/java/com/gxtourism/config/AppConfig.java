package com.gxtourism.config;

import com.gxtourism.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public JwtUtil jwtUtil(@Value("${gxtourism.jwt.secret}") String secret,
                           @Value("${gxtourism.jwt.expire-hours}") long expireHours) {
        return new JwtUtil(secret, expireHours);
    }
}
