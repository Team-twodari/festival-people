package com.wootecam.festivals.domain.festival.service;

import static com.wootecam.festivals.domain.festival.constant.FestivalRedisStreamConstants.FESTIVAL_STREAM_GROUP;
import static com.wootecam.festivals.domain.festival.constant.FestivalRedisStreamConstants.FESTIVAL_STREAM_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wootecam.festivals.domain.festival.entity.Festival;
import com.wootecam.festivals.domain.festival.entity.FestivalProgressStatus;
import com.wootecam.festivals.domain.festival.repository.FestivalRepository;
import com.wootecam.festivals.domain.member.entity.Member;
import com.wootecam.festivals.utils.MemberRepository;
import com.wootecam.festivals.utils.SpringBootTestConfig;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

@RequiredArgsConstructor
class FestivalScheduleConsumerTest extends SpringBootTestConfig {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private FestivalRepository festivalRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Member admin;

    @BeforeEach
    void setup() {
        clear();
        admin = memberRepository.save(
                Member.builder()
                        .name("Test Admin")
                        .email("admin@test.com")
                        .profileImg("profile-img")
                        .build()
        );

        redisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.streamCommands().xTrim(FESTIVAL_STREAM_KEY.getBytes(), 0);
            return null;
        });
    }

    @Test
    @DisplayName("Consumer가 메시지를 정상적으로 처리하는지 확인")
    void testConsumerOnly() throws Exception {
        // Arrange: Manually insert a message into Redis Stream
        LocalDateTime now = LocalDateTime.now();
        Festival festival = festivalRepository.save(Festival.builder()
                .admin(admin)
                .title("Past Festival")
                .description("Festival Description")
                .startTime(now)
                .endTime(now.plusDays(2))
                .festivalProgressStatus(FestivalProgressStatus.UPCOMING)
                .build());

        String festivalJson = objectMapper.writeValueAsString(festival);
        ObjectRecord<String, String> objectRecord = ObjectRecord.create(FESTIVAL_STREAM_KEY, festivalJson);
        redisTemplate.opsForStream().add(objectRecord);

        // Act & Assert: Wait for the Consumer to process the message

        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            PendingMessagesSummary pendingMessagesSummary = redisTemplate.opsForStream()
                    .pending(FESTIVAL_STREAM_KEY, FESTIVAL_STREAM_GROUP);

            assertAll(
                    () -> assertThat(festivalRepository.findById(festival.getId()).get().getFestivalProgressStatus())
                            .isEqualTo(FestivalProgressStatus.ONGOING),
                    () -> assertThat(pendingMessagesSummary.getTotalPendingMessages()).isZero()
            );
        });
    }
}
