package com.wootecam.festivals.global.jwt;

import com.wootecam.festivals.global.auth.AuthErrorCode;
import com.wootecam.festivals.global.exception.WebSocketException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jwts.SIG;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
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

    public static final long ACCESS_TOKEN_VALIDITY = 5 * 60 * 1000L; // 5분

    @Value("${jwt.secret}")
    private String secretKey;

    private SecretKey key;


    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }


    /**
     * JWT 액세스 토큰 생성 (유효시간 설정 가능)
     *
     * @param memberId       사용자 ID
     * @param validityMillis 토큰 유효 시간 (밀리초 단위)
     * @return JWT 액세스 토큰
     */
    public String generateToken(Long memberId, long validityMillis) {
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + validityMillis)) // 유효 시간 동적 설정
                .signWith(this.key, SIG.HS256)
                .compact();
    }


    /**
     * JWT에서 사용자 ID를 추출하고 검증하는 메서드.
     *
     * <p>
     * - JWT 서명을 검증하고, 유효한 경우 사용자 ID를 반환한다. - `verifyWith(key)`를 사용하여 서버에서 발급한 JWT인지 검증한다. - `claims.getSubject()`에서 사용자
     * ID를 추출하여 반환한다. - JWT가 만료되었거나, 올바르지 않은 경우 예외를 발생시킨다.
     * </p>
     *
     * @param token 검증할 JWT 토큰
     * @return 사용자 ID (Long)
     * @throws WebSocketException JWT가 유효하지 않거나 인증 실패 시 예외 발생
     */
    public Long getMemberIdFromToken(String token) {
        Claims claims = extractClaimsFromToken(token);

        return Long.parseLong(claims.getSubject());
    }

    /**
     * JWT에서 Claims(페이로드)를 추출하고 서명을 검증하는 메서드.
     *
     * <p>
     * - JWT의 서명을 검증하고, 유효한 경우 Claims 객체를 반환한다. - 만료되었거나, 올바르지 않은 JWT인 경우 예외를 발생시킨다.
     * </p>
     *
     * @param token 검증할 JWT 토큰
     * @return Claims 객체 (사용자 정보 포함)
     * @throws WebSocketException JWT가 만료되었거나 서명이 올바르지 않은 경우 예외 발생
     */
    private Claims extractClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT 만료됨: {}", e.getMessage());
            throw new WebSocketException(AuthErrorCode.UNAUTHORIZED, "토큰이 만료되었습니다.");
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 형식: {}", e.getMessage());
            throw new WebSocketException(AuthErrorCode.UNAUTHORIZED, "지원되지 않는 JWT 형식입니다.");
        } catch (MalformedJwtException e) {
            log.warn("잘못된 JWT 구조: {}", e.getMessage());
            throw new WebSocketException(AuthErrorCode.UNAUTHORIZED, "JWT 구조가 올바르지 않습니다.");
        } catch (IllegalArgumentException e) {
            log.warn("JWT 처리 중 오류 발생: {}", e.getMessage());
            throw new WebSocketException(AuthErrorCode.UNAUTHORIZED, "JWT 처리 중 오류가 발생했습니다.");
        }
    }

}
