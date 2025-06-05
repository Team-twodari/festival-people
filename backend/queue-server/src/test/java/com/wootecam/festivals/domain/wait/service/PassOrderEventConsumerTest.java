package com.wootecam.festivals.domain.wait.service;

import static com.wootecam.festivals.domain.queue.constant.QueueRedisStreamConstants.PASS_ORDER_STREAM_GROUP;
import static com.wootecam.festivals.domain.queue.constant.QueueRedisStreamConstants.PASS_ORDER_STREAM_KEY;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wootecam.festivals.domain.queue.dto.PassOrderMessage;
import com.wootecam.festivals.global.utils.RedisStreamOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@DisplayName("PassOrderEventConsumer 클래스")
class PassOrderEventConsumerTest {

    private StringRedisTemplate redisTemplate;
    private RedisStreamOperator redisOperator;
    private SimpMessagingTemplate messagingTemplate;
    private WaitOrderService waitOrderService;
    private ObjectMapper objectMapper;
    private PassOrderEventConsumer consumer;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        redisOperator = mock(RedisStreamOperator.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        waitOrderService = mock(WaitOrderService.class);
        objectMapper = new ObjectMapper();
        consumer = new PassOrderEventConsumer(redisTemplate, redisOperator, objectMapper, messagingTemplate, waitOrderService);
    }

    @Test
    @DisplayName("onMessage 메소드는 메시지를 처리하고 ack를 전송한다")
    void on_message_process() throws Exception {
        PassOrderMessage pass = new PassOrderMessage(1L, 3L);
        String json = objectMapper.writeValueAsString(pass);
        RecordId id = RecordId.autoGenerate();
        @SuppressWarnings("unchecked")
        ObjectRecord<String, String> record = (ObjectRecord<String, String>) mock(ObjectRecord.class);
        when(record.getValue()).thenReturn(json);
        when(record.getId()).thenReturn(id);

        StreamOperations<String, Object, Object> ops = mock(StreamOperations.class);
        when(redisTemplate.opsForStream()).thenReturn(ops);

        consumer.onMessage(record);

        verify(waitOrderService).removeWaiting(pass.ticketId(), pass.passOrder());
        verify(messagingTemplate).convertAndSend("/topic/wait/" + pass.ticketId() + "/pass-order", pass.passOrder());
        verify(ops).acknowledge(eq(PASS_ORDER_STREAM_KEY), eq(PASS_ORDER_STREAM_GROUP), eq(id));
    }
}
