package com.wootecam.festivals.domain.wait.session;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * WebSocket 세션과 대기열 정보를 매핑하는 레지스트리.
 */
@Component
public class WaitSessionRegistry {

    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    /**
     * 세션 ID를 사용하여 대기열 참여자의 정보를 등록합니다.
     *
     * @param sessionId WebSocket 세션 ID
     * @param ticketId  대기열에 참여하는 티켓의 ID
     * @param memberId  대기열에 참여하는 사용자의 ID
     * @param order     대기열 참여자의 순서
     */
    public void register(String sessionId, Long ticketId, Long memberId, Long order) {
        sessions.put(sessionId, new SessionInfo(ticketId, memberId, order));
    }

    public SessionInfo remove(String sessionId) {
        return sessions.remove(sessionId);
    }

    public SessionInfo get(String sessionId) {
        return sessions.get(sessionId);
    }

    public List<String> canPassMembersSessionId(Long ticketId) {
        return sessions.values().stream()
                .filter(sessionInfo -> sessionInfo.ticketId().equals(ticketId))
                .map(sessionInfo -> getSessionId(sessionInfo))
                .flatMap(List::stream)
                .toList();
    }

    public List<String> getSessionId(SessionInfo sessionInfo) {
        return sessions.entrySet().stream()
                .filter(entry -> entry.getValue().equals(sessionInfo))
                .map(Map.Entry::getKey)
                .toList();
    }

    /*
     * 세션 정보 클래스.
     * 이 클래스는 티켓 ID와 멤버 ID를 포함하여 대기열 참여자의 정보를 저장합니다.
     * * @param ticketId 대기열에 참여하는 티켓의 ID
     * @param memberId 대기열에 참여하는 사용자의 ID
     */
    public record SessionInfo(Long ticketId, Long memberId, Long order) {
    }
}
