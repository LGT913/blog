package com.blog.blog.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationTime;

    // 构造方法注入，Spring一定会执行
    public JwtUtil(@Value("${jwt.secret-key}") String secretKey,
                   @Value("${jwt.expiration-time}") long expirationTime) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalArgumentException("❌ JWT secret-key 未配置");
        }
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.expirationTime = expirationTime;
        System.out.println("✅ JWT Key 初始化成功");
    }

    public String generateToken(Long userId, String role) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .id(UUID.randomUUID().toString())          // ← jti：JWT 唯一 ID，黑名单 key 用（零碰撞）
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key)
                .compact();
    }

    // 从 token 提取 jti（黑名单 key 用）
    public String getJtiFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getId();
    }

    // token 剩余有效时间（秒），用于设置黑名单 TTL（到期自动清除，不占永久空间）
    public long getRemainingSeconds(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            long diff = claims.getExpiration().getTime() - System.currentTimeMillis();
            return diff > 0 ? diff / 1000 : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.parseLong(claims.getSubject());
    }
     public boolean validateToken(String token) {
        try{
            Jwts.parser().verifyWith(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        }catch(Exception e){
            return false;
        }
     }

    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("role", String.class);
    }
}
