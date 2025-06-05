package com.wootecam.festivals.domain.wait.repository;

import com.wootecam.festivals.domain.ticket.repository.RedisRepository;
import java.util.Map;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 대기열 관리를 위한 Redis Repository. 대기열은 티켓 ID를 기준으로 해시맵 형태로 저장됩니다.
 * - key: ticketId:{ticketId}:waitings
 * - value: userId -> order
 */
@Repository
public class WaitingRedisRepository extends RedisRepository {

    public WaitingRedisRepository(RedisTemplate<String, String> redisTemplate) {
        super(redisTemplate);
    }

    /**
     * 대기열에 사용자를 추가합니다.
     *
     * @param ticketId 티켓 ID
     * @param userId   사용자 ID
     * @return 대기열에서의 순서 (0부터 시작)
     */
    public Long addWaiting(Long ticketId, Long userId) {
        Long getOrder = getSize(ticketId);
        redisTemplate.opsForHash().put(createKey(ticketId), String.valueOf(userId), String.valueOf(getOrder));

        return getOrder;
    }

    /**
     * 대기열 전체 사이즈를 반환합니다.
     */
    public Long getSize(Long ticketId) {
        return redisTemplate.opsForHash().size(createKey(ticketId));
    }

    /**
     * 대기열에 사용자가 존재하는지 확인합니다.
     */
    public Boolean exists(Long ticketId, Long userId) {
        return redisTemplate.opsForHash().hasKey(createKey(ticketId), String.valueOf(userId));
    }

    /**
     * 대기열에서 사용자를 제거합니다.
     */
    public void removeWaiting(Long ticketId, Long userId) {
        redisTemplate.opsForHash().delete(createKey(ticketId), String.valueOf(userId));
    }

    /**
     * 특정 사용자의 순서를 조회합니다.
     */
    public Long getOrder(Long ticketId, Long userId) {
        Object order = redisTemplate.opsForHash().get(createKey(ticketId), String.valueOf(userId));
        return order != null ? Long.parseLong(order.toString()) : null;
    }

    /**
     * 전체 대기열 정보를 Map 형태로 반환합니다.
     */
    public Map<Object, Object> getAll(Long ticketId) {
        return redisTemplate.opsForHash().entries(createKey(ticketId));
    }

    private String createKey(Long ticketId) {
        return TICKETS_PREFIX + ticketId + ":waitings";
    }
}
