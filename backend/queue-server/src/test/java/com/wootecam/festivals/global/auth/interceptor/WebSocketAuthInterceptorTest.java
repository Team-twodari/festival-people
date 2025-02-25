package com.wootecam.festivals.global.auth.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.wootecam.festivals.global.auth.Authentication;
import com.wootecam.festivals.global.jwt.JwtProvider;
import com.wootecam.festivals.global.utils.AuthenticationUtils;
import com.wootecam.festivals.utils.SpringBootTestConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@DisplayName("WebSocketAuthInterceptor 클래스")
class WebSocketAuthInterceptorTest extends SpringBootTestConfig {

    @Autowired
    private WebSocketAuthInterceptor interceptor;

    @Autowired
    private JwtProvider jwtProvider;

    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
    }

    @Nested
    @DisplayName("beforeHandshake 메소드는")
    class Describe_beforeHandshake {

        @Test
        @DisplayName("인증된 사용자는 WebSocket 연결을 허용하고 JWT가 Authorization 헤더에 반환된다.")
        void it_allows_authenticated_user() throws Exception {
            // Given: 인증 정보를 세션에 추가
            Authentication authentication = new Authentication(1L);
            session.setAttribute(AuthenticationUtils.AUTHENTICATION, authentication);

            // Mock HTTP 요청 및 응답 생성
            MockHttpServletRequest servletRequest = new MockHttpServletRequest();
            servletRequest.setSession(session);
            MockHttpServletResponse servletResponse = new MockHttpServletResponse();

            ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
            ServletServerHttpResponse response = new ServletServerHttpResponse(servletResponse);
            Map<String, Object> attributes = new HashMap<>();

            // When: WebSocket 핸드셰이크 실행
            boolean result = interceptor.beforeHandshake(request, response, null, attributes);

            // Then: 인증이 성공하여 핸드셰이크가 허용됨
            assertThat(result).isTrue();

            // Authorization 헤더에 JWT가 포함되었는지 확인
            assertThat(response.getHeaders()).containsKey("Authorization");

            // Authorization 헤더 값 확인 (JWT 토큰이 포함되었는지 검증)
            List<String> authHeaders = response.getHeaders().get("Authorization");
            assertThat(authHeaders).isNotEmpty();
        }

        @Test
        @DisplayName("인증되지 않은 사용자는 WebSocket 연결이 거부된다.")
        void it_denies_unauthenticated_user() throws Exception {
            // Given
            MockHttpServletRequest servletRequest = new MockHttpServletRequest();
            servletRequest.setSession(session);
            MockHttpServletResponse servletResponse = new MockHttpServletResponse();
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));

            ServletServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
            ServletServerHttpResponse response = new ServletServerHttpResponse(servletResponse);
            Map<String, Object> attributes = new HashMap<>();

            // When
            boolean result = interceptor.beforeHandshake(request, response, null, attributes);

            // Then
            assertThat(result).isFalse();
            assertThat(response.getServletResponse().getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }

        @Test
        @DisplayName("request가 ServletServerHttpRequest가 아닐 때 WebSocket 연결이 거부된다.")
        void it_denies_non_http_request() throws Exception {
            // Given: ServerHttpRequest의 Mock 객체 생성
            ServerHttpRequest nonHttpRequest = mock(ServerHttpRequest.class); // Mock 객체 사용
            ServletServerHttpResponse response = new ServletServerHttpResponse(new MockHttpServletResponse());
            Map<String, Object> attributes = new HashMap<>();

            // When
            boolean result = interceptor.beforeHandshake(nonHttpRequest, response, null, attributes);

            // Then
            assertThat(result).isFalse();
            assertThat(response.getServletResponse().getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }
    }
}
