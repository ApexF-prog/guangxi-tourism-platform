package com.gxtourism.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.time.Instant;
import java.util.Date;

/** JWT 签发与校验（HMAC256） */
public class JwtUtil {

    private final String secret;
    private final long expireHours;

    public JwtUtil(String secret, long expireHours) {
        this.secret = secret;
        this.expireHours = expireHours;
    }

    public String sign(Long userId, String username) {
        return JWT.create()
                .withClaim("uid", userId)
                .withClaim("username", username)
                .withExpiresAt(Date.from(Instant.now().plusSeconds(expireHours * 3600)))
                .sign(Algorithm.HMAC256(secret));
    }

    /** 校验失败抛 RuntimeException；成功返回 userId */
    public Long verify(String token) {
        DecodedJWT jwt = JWT.require(Algorithm.HMAC256(secret)).build().verify(token);
        return jwt.getClaim("uid").asLong();
    }
}
