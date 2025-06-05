package com.wootecam.festivals.domain.purchase.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.wootecam.festivals.utils.SpringBootTestConfig;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

@DisplayName("AvailablePurchaseMemberRedisRepository")
class AvailablePurchaseMemberRedisRepositoryTest extends SpringBootTestConfig {

    private final Long ticketId = 1L;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private AvailablePurchaseMemberRedisRepository repository;

    @BeforeEach
    void clearRedis() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @ParameterizedTest
    @ValueSource(longs = {2L, 3L})
    @DisplayName("addAvailableMember 로 추가된 회원은 조회된다")
    void add_and_check(Long memberId) {
        repository.addAvailableMember(ticketId, memberId, 1L);
        assertThat(repository.isAvailable(ticketId, memberId)).isTrue();
    }

    @Test
    @DisplayName("removeAvailableMember 는 회원을 삭제한다")
    void remove_available_member() {
        repository.addAvailableMember(ticketId, 2L, 1L);
        repository.removeAvailableMember(ticketId, 2L);
        assertThat(repository.isAvailable(ticketId, 2L)).isFalse();
    }

    @Test
    @DisplayName("getAllAvailableMembers 는 모든 회원을 반환한다")
    void get_all_members() {
        repository.addAvailableMember(ticketId, 2L, 1L);
        repository.addAvailableMember(ticketId, 3L, 1L);
        Set<String> members = repository.getAllAvailableMembers(ticketId);
        assertThat(members).containsExactlyInAnyOrder("2", "3");
    }

    @Test
    @DisplayName("clear 는 모든 데이터를 삭제한다")
    void clear_all() {
        repository.addAvailableMember(ticketId, 2L, 1L);
        repository.clear(ticketId);
        assertThat(repository.getAllAvailableMembers(ticketId)).isEmpty();
    }
}
