package com.wootecam.festivals.domain.queue.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wootecam.festivals.domain.queue.dto.PassOrderMessage;
import com.wootecam.festivals.global.exception.type.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class PassOrderEventProducerTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private StreamOperations<String, Object, Object> streamOperations;
    @InjectMocks
    private PassOrderEventProducer producer;

    @Test
    @DisplayName("send 는 메시지를 Redis 스트림에 기록한다")
    void send_success() throws Exception {
        PassOrderMessage message = new PassOrderMessage(1L, 2L);
        when(objectMapper.writeValueAsString(message)).thenReturn("{json}");
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        when(streamOperations.add(any(ObjectRecord.class))).thenReturn(RecordId.of("0-1"));

        producer.send(message);

        verify(streamOperations).add(any(ObjectRecord.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"record"})
    @DisplayName("오류 발생 시 ApiException 을 던진다")
    void send_failures(String mode) throws Exception {
        PassOrderMessage message = new PassOrderMessage(1L, 2L);
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
        if (mode.equals("json")) {
            when(objectMapper.writeValueAsString(message)).thenThrow(new JsonProcessingException("err") {
            });
        } else {
            when(objectMapper.writeValueAsString(message)).thenReturn("{json}");
            when(streamOperations.add(any(ObjectRecord.class))).thenReturn(null);
        }
        assertThatThrownBy(() -> producer.send(message)).isInstanceOf(ApiException.class);
    }
}
