package com.wootecam.festivals.global.config;

import com.wootecam.festivals.global.auth.WebSocketAuthArgumentResolver;
import com.wootecam.festivals.global.auth.interceptor.WebSocketAuthInterceptor;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 및 STOMP 설정을 담당하는 클래스.
 *
 * <p>
 * - WebSocket을 사용하여 클라이언트와 서버 간의 실시간 통신을 지원합니다. - STOMP 프로토콜을 활용한 Pub/Sub 모델을 적용하여 메시지 브로커를 구성합니다. - Spring Session을
 * 활용하여 WebSocket과 HTTP 세션을 공유할 수 있도록 설정합니다. - `AuthArgumentResolver`를 추가하여 컨트롤러 핸들러 메서드에서 인증된 사용자 정보를 주입할 수 있도록 지원합니다.
 * - `heartbeat` 설정을 추가하여 클라이언트와 서버 간의 연결을 유지하고, 비정상 종료된 세션을 감지할 수 있도록 합니다.
 * </p>
 *
 * @author 김현준
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthArgumentResolver authArgumentResolver;
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    /**
     * WebSocket 엔드포인트를 등록하여 클라이언트가 서버와 연결할 수 있도록 설정합니다.
     *
     * <p>
     * - 클라이언트는 `ws://server.com/ws`를 통해 WebSocket에 연결할 수 있습니다. - CORS 허용을 위해 `setAllowedOriginPatterns("*")`을 추가하여 다양한
     * 도메인에서 접근할 수 있도록 설정합니다. - Spring Session을 활용하여 HTTP 세션과 WebSocket 세션을 공유할 수 있도록 구성됩니다.
     * </p>
     *
     * @param registry STOMP 엔드포인트를 등록할 레지스트리 객체
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws") // 클라이언트가 연결할 WebSocket 엔드포인트
                .setAllowedOriginPatterns("*") // CORS 설정: 모든 도메인에서 접근 가능
                .addInterceptors(webSocketAuthInterceptor);
    }

    /**
     * 메시지 브로커를 설정하여 클라이언트 간 메시지 전송을 가능하게 합니다.
     *
     * <p>
     * - `/app`으로 시작하는 메시지는 애플리케이션 내부에서 처리됩니다. - `/topic`과 `/queue`로 시작하는 메시지는 메시지 브로커를 통해 전달됩니다. - `/user/queue/`는 특정
     * 사용자에게 메시지를 보내는 데 사용됩니다. - `heartbeat` 설정을 추가하여 서버와 클라이언트 간 연결을 유지하고, 비정상 종료된 세션을 감지할 수 있도록 합니다.
     * </p>
     *
     * @param registry 메시지 브로커 설정 객체
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue") // 메시지 브로커 활성화
                .setHeartbeatValue(new long[]{10000, 10000}) // 10초마다 하트비트 전송 설정
                .setTaskScheduler(heartBeatTaskScheduler()); // 하트비트 관리용 스레드 풀 사용
        registry.setApplicationDestinationPrefixes("/app"); // 클라이언트가 보낼 메시지의 prefix 설정
        registry.setUserDestinationPrefix("/user"); // 특정 사용자에게 메시지를 보낼 때 사용
    }

    /**
     * WebSocket 메시지 핸들러에서 사용자 인증 정보를 자동으로 주입하는 Argument Resolver를 추가합니다.
     *
     * <p>
     * - `@AuthUser` 어노테이션이 있는 컨트롤러의 파라미터에 `memberId`를 자동 주입하여 활용할 수 있습니다.
     * </p>
     *
     * @param argumentResolvers 메시지 핸들러에서 사용할 Argument Resolver 리스트
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(authArgumentResolver);
    }

    /**
     * TaskScheduler 빈을 등록하여 WebSocket의 Heartbeat 기능을 지원합니다.
     *
     * <p>
     * - `heartbeat`을 설정하면 주기적으로 서버와 클라이언트 간의 연결 상태를 확인해야 합니다. - Spring의 `ThreadPoolTaskScheduler`를 사용하여 heartbeat 작업을
     * 처리합니다. - 풀 크기를 5로 설정하여 다중 연결을 효율적으로 관리하도록 구성합니다.
     * </p>
     *
     * @return `ThreadPoolTaskScheduler` 인스턴스
     */
    @Bean
    public ThreadPoolTaskScheduler heartBeatTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("wss-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }
}
