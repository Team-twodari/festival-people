package com.wootecam.festivals.domain.wait.repository;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AvailablePurchaseMemberRedisRepository {

    private final RedisTemplate<String, String> redisTemplate;

    private String createKey(Long ticketId) {
        return "tickets:" + ticketId + ":availableMembers";
    }

    /**
     * 구매 가능 유저 추가
     */
    public void addAvailableMember(Long ticketId, Long memberId, Long ttl) {
        String key = createKey(ticketId);
        redisTemplate.opsForSet().add(key, String.valueOf(memberId));
        redisTemplate.expire(key, ttl, TimeUnit.MINUTES);
    }

    /**
     * 구매 가능 유저 제거
     */
    public void removeAvailableMember(Long ticketId, Long memberId) {
        redisTemplate.opsForSet().remove(createKey(ticketId), String.valueOf(memberId));
    }

    /**
     * 특정 유저가 구매 가능 목록에 있는지 확인
     */
    public boolean isAvailable(Long ticketId, Long memberId) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForSet().isMember(createKey(ticketId), String.valueOf(memberId))
        );
    }

    /**
     * 구매 가능 유저 전체 조회
     */
    public Set<String> getAllAvailableMembers(Long ticketId) {
        return redisTemplate.opsForSet().members(createKey(ticketId));
    }

    /**
     * 전체 제거 (필요시)
     */
    public void clear(Long ticketId) {
        redisTemplate.delete(createKey(ticketId));
    }
}
