package com.wootecam.festivals.domain.wait.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.wootecam.festivals.utils.SpringBootTestConfig;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

@DisplayName("WaitingRedisRepository 클래스")
class WaitingRedisRepositoryTest extends SpringBootTestConfig {

    private final Long ticketId = 1L;
    private final Long memberId = 2L;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private WaitingRedisRepository repository;

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    @DisplayName("addWaiting 은 대기열에 사용자를 추가하고 순서를 반환한다")
    void add_waiting() {
        Long order = repository.addWaiting(ticketId, memberId);
        assertThat(order).isZero();
        assertThat(repository.getSize(ticketId)).isEqualTo(1L);
    }

    @Test
    @DisplayName("exists 는 대기열 존재 여부를 확인한다")
    void exists_waiting() {
        repository.addWaiting(ticketId, memberId);
        assertThat(repository.exists(ticketId, memberId)).isTrue();
    }

    @Test
    @DisplayName("getOrder 는 사용자의 대기 순서를 반환한다")
    void get_order() {
        repository.addWaiting(ticketId, memberId);
        assertThat(repository.getOrder(ticketId, memberId)).isEqualTo(0L);
    }

    @Test
    @DisplayName("removeWaiting 은 대기열에서 사용자를 제거한다")
    void remove_waiting() {
        repository.addWaiting(ticketId, memberId);
        repository.removeWaiting(ticketId, memberId);
        assertThat(repository.exists(ticketId, memberId)).isFalse();
    }

    @Test
    @DisplayName("getAll 은 대기열의 모든 사용자를 반환한다")
    void get_all() {
        repository.addWaiting(ticketId, memberId);
        Map<Object, Object> all = repository.getAll(ticketId);
        assertThat(all).containsEntry("2", "0");
    }
}
