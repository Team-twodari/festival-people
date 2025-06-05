package com.wootecam.festivals.domain.wait.session;

import com.wootecam.festivals.domain.wait.repository.WaitingRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * WebSocket 세션 종료 시 대기열 정보를 정리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WaitWebSocketEventListener {

    private final WaitSessionRegistry sessionRegistry;
    private final WaitingRedisRepository waitingRepository;

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        WaitSessionRegistry.SessionInfo info = sessionRegistry.remove(sessionId);
        if (info != null) {
            waitingRepository.removeWaiting(info.ticketId(), info.memberId());
            log.debug("세션 종료 - ticketId: {}, memberId: {}", info.ticketId(), info.memberId());
        }
    }
}
