package com.wootecam.festivals.global.auth;

import static com.wootecam.festivals.global.jwt.JwtProvider.ACCESS_TOKEN_VALIDITY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.wootecam.festivals.global.exception.WebSocketException;
import com.wootecam.festivals.global.jwt.JwtProvider;
import com.wootecam.festivals.utils.SpringBootTestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

@DisplayName("WebSocketAuthArgumentResolver 테스트")
class WebSocketAuthArgumentResolverTest extends SpringBootTestConfig {

    @Autowired
    private WebSocketAuthArgumentResolver resolver;

    @Autowired
    private JwtProvider jwtProvider;

    @Mock
    private MethodParameter parameter;

    @Test
    @DisplayName("JWT가 포함된 Authorization 헤더가 있으면 사용자 ID를 반환한다.")
    void resolveArgument_validJwt_returnsUserId() throws Exception {
        // Given
        Long memberId = 123L;
        String validToken = jwtProvider.generateToken(memberId, ACCESS_TOKEN_VALIDITY); // 정상적인 JWT 생성

        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        accessor.setNativeHeader("Authorization", "Bearer " + validToken);
        Message<byte[]> message = MessageBuilder.withPayload(new byte[0])
                .setHeaders(accessor)
                .build();

        // When
        Object resolvedId = resolver.resolveArgument(parameter, message);

        // Then
        assertThat(resolvedId).isEqualTo(memberId);
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 예외가 발생한다.")
    void resolveArgument_noAuthHeader_throwsException() {
        // Given
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        Message<byte[]> message = MessageBuilder.withPayload(new byte[0])
                .setHeaders(accessor)
                .build();

        // When & Then
        assertThrows(WebSocketException.class, () -> resolver.resolveArgument(parameter, message));
    }

    @Test
    @DisplayName("Authorization 헤더가 'Bearer '로 시작하지 않으면 예외가 발생한다.")
    void resolveArgument_invalidAuthHeader_throwsException() {
        // Given
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        accessor.setNativeHeader("Authorization", "InvalidTokenFormat");
        Message<byte[]> message = MessageBuilder.withPayload(new byte[0])
                .setHeaders(accessor)
                .build();

        // When & Then
        assertThrows(WebSocketException.class, () -> resolver.resolveArgument(parameter, message));
    }

    @Test
    @DisplayName("만료된 JWT가 포함되었을 때 예외가 발생한다.")
    void resolveArgument_expiredJwt_throwsException() {
        // Given
        Long memberId = 123L;
        String expiredToken = createExpiredToken(memberId); // 만료된 JWT 생성

        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        accessor.setNativeHeader("Authorization", "Bearer " + expiredToken);
        Message<byte[]> message = MessageBuilder.withPayload(new byte[0])
                .setHeaders(accessor)
                .build();

        // When & Then
        assertThrows(WebSocketException.class, () -> resolver.resolveArgument(parameter, message));
    }

    /**
     * 1초 전에 만료된 JWT를 생성하는 메서드.
     *
     * @param memberId 사용자 ID
     * @return 1초 전에 만료된 JWT
     */
    private String createExpiredToken(Long memberId) {
        return jwtProvider.generateToken(memberId, -1000L);
    }

    // --------------- supportsParameter() 메서드 테스트 ---------------

    @Test
    @DisplayName("@AuthUser 어노테이션이 있고 Long 타입이면 true를 반환한다.")
    void supportsParameter_withAuthUserAndLongType_returnsTrue() {
        // Given
        MethodParameter mockParameter = mock(MethodParameter.class);
        when(mockParameter.hasParameterAnnotation(AuthUser.class)).thenReturn(true);
        when(mockParameter.getParameterType()).thenReturn((Class) Long.class);

        // When
        boolean result = resolver.supportsParameter(mockParameter);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("@AuthUser 어노테이션이 없으면 false를 반환한다.")
    void supportsParameter_withoutAuthUserAnnotation_returnsFalse() {
        // Given
        MethodParameter mockParameter = mock(MethodParameter.class);
        when(mockParameter.hasParameterAnnotation(AuthUser.class)).thenReturn(false);
        when(mockParameter.getParameterType()).thenReturn((Class) Long.class);

        // When
        boolean result = resolver.supportsParameter(mockParameter);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Long 타입이 아니면 false를 반환한다.")
    void supportsParameter_withNonLongType_returnsFalse() {
        // Given
        MethodParameter mockParameter = mock(MethodParameter.class);
        when(mockParameter.hasParameterAnnotation(AuthUser.class)).thenReturn(true);
        when(mockParameter.getParameterType()).thenReturn((Class) String.class);

        // When
        boolean result = resolver.supportsParameter(mockParameter);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Long 타입이고 @AuthUser가 있으면 true를 반환한다.")
    void supportsParameter_withLongTypeAndAuthUser_returnsTrue() {
        // Given
        MethodParameter mockParameter = mock(MethodParameter.class);
        when(mockParameter.hasParameterAnnotation(AuthUser.class)).thenReturn(true);
        when(mockParameter.getParameterType()).thenReturn((Class) Long.class);

        // When
        boolean result = resolver.supportsParameter(mockParameter);

        // Then
        assertThat(result).isTrue();
    }

}
