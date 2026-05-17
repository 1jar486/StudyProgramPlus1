package com.zhh.taskmanager.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

    private static final Key KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // 短效 Access Token 有效期：2 小时
    private static final long ACCESS_TOKEN_EXPIRATION = 2 * 60 * 60 * 1000L;
    // 长效 Refresh Token 有效期：7 天
    private static final long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 60 * 60 * 1000L;

    private static final String TOKEN_PREFIX = "SUCCESS_TOKEN_";

    /**
     * 生成短效 Access Token（携带用户ID）
     */
    public String generateAccessToken(String username, Long userId) {
        return TOKEN_PREFIX + Jwts.builder()
                .setSubject(username)
                .claim("userId", userId) // 🌟 核心改动：把用户唯一ID存进JWT载荷中
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION))
                .signWith(KEY)
                .compact();
    }

    /**
     * 生成长效 Refresh Token（携带用户ID）
     */
    public String generateRefreshToken(String username, Long userId) {
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId) // 🌟 核心改动：把用户唯一ID存进JWT载荷中
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
                .signWith(KEY)
                .compact();
    }

    /**
     * 解析并验证 Token
     */
    public Claims parseToken(String token) {
        if (token.startsWith(TOKEN_PREFIX)) {
            token = token.substring(TOKEN_PREFIX.length());
        }
        return Jwts.parserBuilder()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 从 Token 中提取用户 ID
     */
    public Long getUserIdFromToken(String token) {
        return parseToken(token).get("userId", Long.class);
    }

    /**
     * 从 Token 中提取用户名
     */
    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }
}