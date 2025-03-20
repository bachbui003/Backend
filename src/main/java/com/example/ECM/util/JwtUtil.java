package com.example.ECM.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
    private static final String SECRET_KEY = "your-secret-key-your-secret-key-your-secret-key";
    private static final long EXPIRATION_TIME = 86400000; // 24h

    private final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    // 🔹 Tạo token chứa thêm role, phone, address, email, fullName
    public String generateToken(String username, String role, String phone, String address, String email, String fullName) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("fullName", fullName);
        claims.put("phone", phone);
        claims.put("address", address);
        claims.put("email", email);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 🔹 Lấy username từ token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    // 🔹 Lấy role từ token
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }
    // 🔹 Lấy phone từ token
    public String extractPhone(String token) {
        return extractClaim(token, claims -> claims.get("phone", String.class));
    }
    // 🔹 Lấy address từ token
    public String extractAddress(String token) {
        return extractClaim(token, claims -> claims.get("address", String.class));
    }
    // 🔹 Lấy email từ token
    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    // 🔹 Lấy một claim từ token
    private <T> T extractClaim(String token, java.util.function.Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    // 🔹 Lấy fullName từ token
    public String extractFullName(String token) {
        return extractClaim(token, claims -> claims.get("fullName", String.class));
    }

    // 🔹 Lấy toàn bộ claims từ token
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    // 🔹 Kiểm tra token hợp lệ
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
