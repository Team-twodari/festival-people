package com.wootecam.festivals.global.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.wootecam.festivals.global.auth.AuthErrorCode;
import com.wootecam.festivals.global.exception.WebSocketException;
import com.wootecam.festivals.utils.SpringBootTestConfig;
import io.jsonwebtoken.Jwts;
import java.util.Date;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class JwtProviderTest extends SpringBootTestConfig {

    public static final long ACCESS_TOKEN_VALIDITY = JwtProvider.ACCESS_TOKEN_VALIDITY; // 5분

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("JWT를 정상적으로 생성할 수 있다.")
    void it_generates_valid_jwt() {
        // Given
        Long memberId = 1L;

        // When
        String token = jwtProvider.generateToken(memberId, ACCESS_TOKEN_VALIDITY);

        // Then
        assertThat(token).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("유효한 JWT에서 사용자 ID를 정상적으로 추출할 수 있다.")
    void it_extracts_member_id_from_valid_token() {
        // Given
        Long memberId = 1L;
        String token = jwtProvider.generateToken(memberId, ACCESS_TOKEN_VALIDITY);

        // When
        Long extractedMemberId = jwtProvider.getMemberIdFromToken(token);

        // Then
        assertThat(extractedMemberId).isEqualTo(memberId);
    }

    @Test
    @DisplayName("만료된 JWT를 검증할 때 예외가 발생해야 한다.")
    void it_throws_exception_when_token_is_expired() {
        // Given: 즉시 만료된 JWT 생성
        Long memberId = 1L;
        String expiredToken = jwtProvider.generateToken(memberId, -1000L);

        // When & Then
        WebSocketException exception = assertThrows(WebSocketException.class, () -> {
            jwtProvider.getMemberIdFromToken(expiredToken);
        });

        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.UNAUTHORIZED);
        assertThat(exception.getMessage()).contains("토큰이 만료되었습니다.");
    }

    @Test
    @DisplayName("지원되지 않는 JWT 형식을 검증할 때 예외가 발생해야 한다.")
    void it_throws_exception_when_token_is_unsupported() {
        // Given: 임의의 잘못된 JWT 구조
        String unsupportedToken = Jwts.builder()
                .subject("1")
                .issuedAt(new Date())
                .compact(); // 서명이 없는 JWT

        // When & Then
        WebSocketException exception = assertThrows(WebSocketException.class, () -> {
            jwtProvider.getMemberIdFromToken(unsupportedToken);
        });

        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.UNAUTHORIZED);
        assertThat(exception.getMessage()).contains("지원되지 않는 JWT 형식입니다.");
    }

    @Test
    @DisplayName("잘못된 구조의 JWT를 검증할 때 예외가 발생해야 한다.")
    void it_throws_exception_when_token_is_malformed() {
        // Given
        String malformedToken = "malformed.jwt.token";

        // When & Then
        WebSocketException exception = assertThrows(WebSocketException.class, () -> {
            jwtProvider.getMemberIdFromToken(malformedToken);
        });

        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.UNAUTHORIZED);
        assertThat(exception.getMessage()).contains("JWT 구조가 올바르지 않습니다.");
    }

    @Test
    @DisplayName("비어있는 JWT를 검증할 때 예외가 발생해야 한다.")
    void it_throws_exception_when_token_is_empty() {
        // Given
        String emptyToken = "";

        // When & Then
        WebSocketException exception = assertThrows(WebSocketException.class, () -> {
            jwtProvider.getMemberIdFromToken(emptyToken);
        });

        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.UNAUTHORIZED);
        assertThat(exception.getMessage()).contains("JWT 처리 중 오류가 발생했습니다.");
    }

    @Test
    @DisplayName("null JWT를 검증할 때 예외가 발생해야 한다.")
    void it_throws_exception_when_token_is_null() {
        // Given
        String nullToken = null;

        // When & Then
        WebSocketException exception = assertThrows(WebSocketException.class, () -> {
            jwtProvider.getMemberIdFromToken(nullToken);
        });

        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.UNAUTHORIZED);
        assertThat(exception.getMessage()).contains("JWT 처리 중 오류가 발생했습니다.");
    }
}
