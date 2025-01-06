package com.wootecam.festivals.domain.festival.service;

import static com.wootecam.festivals.domain.festival.constant.FestivalRedisStreamConstants.FESTIVAL_STREAM_KEY;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wootecam.festivals.domain.festival.entity.Festival;
import com.wootecam.festivals.global.exception.GlobalErrorCode;
import com.wootecam.festivals.global.exception.type.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FestivalScheduleEventProducer {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void sendEvent(Festival festival) {
        log.info("Send event to redis: {}", festival);

        try {
            String festivalToJson = objectMapper.writeValueAsString(festival);

            ObjectRecord<String, String> message = StreamRecords.newRecord()
                    .ofObject(festivalToJson)
                    .withStreamKey(FESTIVAL_STREAM_KEY);

            RecordId recordId = redisTemplate.opsForStream().add(message);

            if (recordId == null) {
                log.error("Failed to send festival event to stream");
                throw new ApiException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
            }

            log.info("Event sent to redis festivalId: {}", festival.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to send festival event to stream", e);
            throw new ApiException(GlobalErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }
}
