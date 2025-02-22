package com.wootecam.festivals.global.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 토큰을 관리하는 클래스.
 * <p>
 * - 최신 io.jsonwebtoken 라이브러리를 사용하여 JWT 생성 및 검증 기능을 제공한다. - 서명 검증을 `verifyWith()` 메서드를 통해 처리.
 * </p>
 *
 * @author 김현준
 */
@Slf4j
@Component
public class JwtProvider {

    private static final long ACCESS_TOKEN_VALIDITY = 60 * 60 * 1000L; // 1시간

    @Value("${jwt.secret}")
    private String secretKey;

    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * JWT 액세스 토큰 생성.
     *
     * @param memberId 사용자 ID
     * @return JWT 액세스 토큰
     */
    public String generateToken(Long memberId) {
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_VALIDITY))
                .signWith(key)
                .compact();
    }
}
