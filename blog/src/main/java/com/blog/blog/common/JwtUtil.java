package com.blog.blog.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;

@Component
public class JwtUtil {
    // 密钥（自定义一个复杂的字符串，越长越安全）
    private static final String SECRET_KEY = "MySecretKeyForJWTTokenGeneration2026BlogProject";

    // 过期时间（24小时，单位：毫秒）
    private static final long EXPIRATION_TIME = 86400000;

    public static String generateToken(Long userId) {
        SecretKey key= Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder().subject(userId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis()+EXPIRATION_TIME))
                .signWith(key)
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        SecretKey key= Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.parseLong(claims.getSubject());
    }
     public boolean validateToken(String token) {
        try{
            SecretKey key= Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
            Jwts.parser().verifyWith(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        }catch(Exception e){
            return false;
        }
     }
}
