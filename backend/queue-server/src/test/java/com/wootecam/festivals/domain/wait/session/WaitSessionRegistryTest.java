package com.wootecam.festivals.domain.wait.session;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("WaitSessionRegistry 클래스")
class WaitSessionRegistryTest {

    private WaitSessionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new WaitSessionRegistry();
    }

    @Test
    @DisplayName("register 메소드는 세션 정보를 등록하고 조회할 수 있다")
    void register_and_get() {
        registry.register("s1", 1L, 2L, 3L);
        WaitSessionRegistry.SessionInfo info = registry.get("s1");
        assertThat(info.ticketId()).isEqualTo(1L);
        assertThat(info.memberId()).isEqualTo(2L);
        assertThat(info.order()).isEqualTo(3L);
    }

    @Test
    @DisplayName("remove 메소드는 세션 정보를 삭제한다")
    void remove_session() {
        registry.register("s1", 1L, 2L, 3L);
        registry.remove("s1");
        assertThat(registry.get("s1")).isNull();
    }

    @Test
    @DisplayName("canPassMembersSessionId 메소드는 티켓에 속한 세션 아이디를 반환한다")
    void can_pass_members_session_id() {
        registry.register("s1", 1L, 2L, 3L);
        registry.register("s2", 1L, 3L, 4L);
        registry.register("s3", 2L, 4L, 5L);

        List<String> ids = registry.canPassMembersSessionId(1L);
        assertThat(ids).containsExactlyInAnyOrder("s1", "s2");
    }
}
