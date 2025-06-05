package com.wootecam.festivals.domain.wait.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.wootecam.festivals.utils.SpringBootTestConfig;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

@DisplayName("AvailablePurchaseMemberRedisRepository 클래스")
class AvailablePurchaseMemberRedisRepositoryTest extends SpringBootTestConfig {

    private final Long ticketId = 1L;
    private final Long memberId = 2L;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private AvailablePurchaseMemberRedisRepository repository;

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    @DisplayName("addAvailableMember 는 구매 가능 유저를 추가한다")
    void add_available_member() {
        repository.addAvailableMember(ticketId, memberId, 1L);
        assertThat(repository.isAvailable(ticketId, memberId)).isTrue();
    }

    @Test
    @DisplayName("removeAvailableMember 는 구매 가능 유저를 제거한다")
    void remove_available_member() {
        repository.addAvailableMember(ticketId, memberId, 1L);
        repository.removeAvailableMember(ticketId, memberId);
        assertThat(repository.isAvailable(ticketId, memberId)).isFalse();
    }

    @Test
    @DisplayName("getAllAvailableMembers 는 전체 구매 가능 유저를 조회한다")
    void get_all_available_members() {
        repository.addAvailableMember(ticketId, memberId, 1L);
        repository.addAvailableMember(ticketId, 3L, 1L);
        Set<String> members = repository.getAllAvailableMembers(ticketId);
        assertThat(members).containsExactlyInAnyOrder("2", "3");
    }

    @Test
    @DisplayName("clear 는 모든 구매 가능 유저를 삭제한다")
    void clear_all() {
        repository.addAvailableMember(ticketId, memberId, 1L);
        repository.clear(ticketId);
        assertThat(repository.getAllAvailableMembers(ticketId)).isEmpty();
    }
}
