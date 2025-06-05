package com.wootecam.festivals.domain.queue.service;


import static com.wootecam.festivals.domain.queue.constant.QueueRedisStreamConstants.PASS_ORDER_STREAM_KEY;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wootecam.festivals.domain.queue.dto.PassOrderMessage;
import com.wootecam.festivals.global.exception.GlobalErrorCode;
import com.wootecam.festivals.global.exception.type.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@DependsOn(value = {"redisConnectionFactory", "redisStreamInitializer"})
@RequiredArgsConstructor
public class PassOrderEventProducer {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void send(PassOrderMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            ObjectRecord<String, String> record = StreamRecords.newRecord().ofObject(json)
                    .withStreamKey(PASS_ORDER_STREAM_KEY);
            RecordId recordId = redisTemplate.opsForStream().add(record);
            if (recordId == null) {
                throw new ApiException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
            }
            log.debug("pass order event sent: {}", message);
        } catch (JsonProcessingException e) {
            throw new ApiException(GlobalErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }
}
