package com.wootecam.festivals.global.auth;

import com.wootecam.festivals.global.exception.WebSocketException;
import com.wootecam.festivals.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * WebSocket 메시지 핸들러에서 인증된 사용자 정보를 자동으로 주입하는 Argument Resolver.
 *
 * <p>
 * - WebSocket 메시지에서 `Authorization` 헤더에 포함된 JWT를 검증하여 사용자 정보를 추출한다. - 인증된 사용자의 `memberId`를 컨트롤러 메서드의 `@AuthUser` 파라미터로
 * 자동 주입한다. - 인증되지 않은 사용자는 예외를 발생시켜 메시지 처리를 차단한다.
 * </p>
 *
 * @author 김현준
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketAuthArgumentResolver implements HandlerMethodArgumentResolver {

    private final JwtProvider jwtProvider;

    /**
     * 해당 파라미터가 `@AuthUser` 어노테이션이 붙어 있고, `Long` 타입이면 지원하는지 확인.
     *
     * <p>
     * - WebSocket 컨트롤러의 특정 파라미터가 `@AuthUser` 어노테이션을 포함하고 있는지 확인한다. - 파라미터 타입이 `Long`이면 이 Argument Resolver가 처리하도록
     * 허용한다.
     * </p>
     *
     * @param parameter 검사할 메서드 파라미터
     * @return `@AuthUser`가 선언된 `Long` 타입 파라미터이면 `true`, 그렇지 않으면 `false`
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthUser.class) &&
                Long.class.isAssignableFrom(parameter.getParameterType());
    }

    /**
     * WebSocket 메시지에서 인증 정보를 가져오는 Argument Resolver.
     *
     * <p>
     * - WebSocket 메시지 헤더에서 `Authorization` 값을 추출하여 JWT를 검증한다. - `Authorization` 헤더의 값이 "Bearer {JWT}" 형식인지 확인한다. -
     * JWT에서 사용자 ID를 추출하고 컨트롤러 메서드의 `@AuthUser` 파라미터에 전달한다. - 인증되지 않은 사용자는 `WebSocketException`을 발생시켜 메시지 처리를 차단한다.
     * </p>
     *
     * @param parameter 컨트롤러 메서드의 파라미터 정보
     * @param message   WebSocket 메시지 객체
     * @return 인증된 사용자의 `memberId` (`Long` 타입)
     * @throws WebSocketException JWT가 없거나 유효하지 않은 경우 예외 발생
     */
    @Override
    public Object resolveArgument(MethodParameter parameter, Message<?> message) throws Exception {
        // WebSocket 메시지 헤더에서 `Authorization` 값 가져오기
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
        String authHeader = accessor.getFirstNativeHeader("Authorization");

        // JWT 토큰이 없거나 "Bearer " 형식이 아닌 경우 예외 발생
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new WebSocketException(AuthErrorCode.UNAUTHORIZED, "인증 토큰이 존재하지 않습니다.");
        }

        // "Bearer " 접두사를 제거하고 JWT 토큰만 추출
        String token = authHeader.substring(7);

        // JWT에서 사용자 ID 추출 및 반환
        return jwtProvider.getMemberIdFromToken(token);
    }

}
