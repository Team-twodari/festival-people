package com.wootecam.festivals.global.config;

import static com.wootecam.festivals.domain.festival.constant.FestivalRedisStreamConstants.FESTIVAL_STREAM_GROUP;
import static com.wootecam.festivals.domain.festival.constant.FestivalRedisStreamConstants.FESTIVAL_STREAM_KEY;
import static com.wootecam.festivals.domain.ticket.constant.TicketRedisStreamConstants.TICKET_STREAM_GROUP;
import static com.wootecam.festivals.domain.ticket.constant.TicketRedisStreamConstants.TICKET_STREAM_KEY;

import com.wootecam.festivals.global.utils.RedisStreamOperator;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisStreamInitializer {

    private static final String LOCK_KEY = "lock_key";

    private final StringRedisTemplate redisTemplate;
    private final RedisStreamOperator redisStreamOperator;

    @PostConstruct
    public void initStreams() {
        // 초기화 동시 실행 방지
        boolean locked = Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, "locked", Duration.ofSeconds(30)));
        if (!locked) {
            log.info("다른 프로세스에서 이미 초기화 중입니다. 초기화를 건너뜁니다.");
            return;
        }

        // 스트림 및 소비자 그룹 초기화
        initializeStream(FESTIVAL_STREAM_KEY, FESTIVAL_STREAM_GROUP);
        initializeStream(TICKET_STREAM_KEY, TICKET_STREAM_GROUP);
    }

    private void initializeStream(String streamKey, String groupName) {
        log.info("Stream {} 초기화 시도", streamKey);
        log.info("Consumer Group {} 초기화 시도", groupName);

        redisStreamOperator.createStreamConsumerGroup(streamKey, groupName);
    }
}
