package com.wootecam.festivals.global.auth.interceptor;

import static com.wootecam.festivals.global.jwt.JwtProvider.ACCESS_TOKEN_VALIDITY;

import com.wootecam.festivals.global.auth.Authentication;
import com.wootecam.festivals.global.jwt.JwtProvider;
import com.wootecam.festivals.global.utils.AuthenticationUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * WebSocket 연결 시 인증을 처리하는 인터셉터.
 * <p>
 * - WebSocket 핸드셰이크 과정에서 사용자의 인증 정보를 확인하고, 인증되지 않은 사용자의 연결을 차단한다. - 인증된 사용자의 정보를 WebSocket 세션 속성에 저장하여 이후 WebSocket
 * 통신에서 활용할 수 있도록 한다.
 * </p>
 *
 * @author 김현준
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtProvider jwtProvider;

    /**
     * WebSocket 핸드셰이크 요청을 가로채어 인증을 수행하는 메서드.
     * <p>
     * - 인증된 사용자의 ID를 WebSocket 세션 속성에 저장. - 인증되지 않은 사용자는 HTTP 401 응답을 반환하며 연결을 차단.
     * </p>
     *
     * @param request    WebSocket 핸드셰이크 요청 객체
     * @param response   WebSocket 핸드셰이크 응답 객체
     * @param wsHandler  WebSocket 핸들러
     * @param attributes WebSocket 세션 속성 (인증된 사용자 ID 저장)
     * @return 인증이 성공하면 true, 실패하면 false
     * @throws Exception 예외 발생 시 WebSocket 연결 차단
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (!(request instanceof ServletServerHttpRequest servletServerHttpRequest)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        HttpServletRequest httpServletRequest = servletServerHttpRequest.getServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpServletRequest));

        Authentication authentication = AuthenticationUtils.getAuthentication();

        if (authentication == null) {
            log.warn("WebSocket 인증 실패 - 인증 정보 없음");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        // JWT 생성 및 응답 헤더에 추가
        String jwtToken = jwtProvider.generateToken(authentication.memberId(), ACCESS_TOKEN_VALIDITY);
        response.getHeaders().add("Authorization", jwtToken);

        log.info("WebSocket 인증 성공: {}", authentication);

        return true;
    }


    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                               Exception exception) {
    }
}
