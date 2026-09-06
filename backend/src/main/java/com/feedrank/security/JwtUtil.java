package com.feedrank.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {
  private final SecretKey key;
  private final long expirationMs;

  public JwtUtil(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.expiration-ms}") long expirationMs) {
    byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
    if (bytes.length < 32) {
      byte[] padded = new byte[32];
      System.arraycopy(bytes, 0, padded, 0, bytes.length);
      bytes = padded;
    }
    this.key = Keys.hmacShaKeyFor(bytes);
    this.expirationMs = expirationMs;
  }

  public String generate(Long userId, String username) {
    Date now = new Date();
    return Jwts.builder()
        .subject(String.valueOf(userId))
        .claim("username", username)
        .issuedAt(now)
        .expiration(new Date(now.getTime() + expirationMs))
        .signWith(key)
        .compact();
  }

  public Long parseUserId(String token) {
    return Long.valueOf(Jwts.parser().verifyWith(key).build()
        .parseSignedClaims(token).getPayload().getSubject());
  }

  public String parseUsername(String token) {
    Object v = Jwts.parser().verifyWith(key).build()
        .parseSignedClaims(token).getPayload().get("username");
    return v == null ? null : String.valueOf(v);
  }
}
