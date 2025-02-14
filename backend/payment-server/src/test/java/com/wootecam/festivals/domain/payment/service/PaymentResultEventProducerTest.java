package com.wootecam.festivals.domain.payment.service;

import static com.wootecam.festivals.domain.payment.constant.PaymentRedisStreamConstants.PAYMENT_RESULT_STREAM_KEY;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wootecam.festivals.domain.payment.dto.PaymentResult;
import com.wootecam.festivals.domain.payment.entity.PaymentStatus;
import com.wootecam.festivals.global.exception.GlobalErrorCode;
import com.wootecam.festivals.global.exception.type.ApiException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
@DisplayName("PaymentResultEventProducer 통합 테스트")
class PaymentResultEventProducerTest {

    @Autowired
    private PaymentResultEventProducer paymentResultEventProducer;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        redisTemplate.delete(PAYMENT_RESULT_STREAM_KEY);
    }

    @Test
    @DisplayName("정상적인 payment result event 전송 테스트")
    void testSendPaymentResultEventSuccess() {
        // given
        PaymentResult paymentResult = new PaymentResult("testPaymentId", PaymentStatus.SUCCESS);

        // when
        assertDoesNotThrow(() -> paymentResultEventProducer.sendPaymentResultEvent(paymentResult));

        // then
        List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream()
                .range(PAYMENT_RESULT_STREAM_KEY, Range.unbounded());
        assertNotNull(records, "Redis 스트림 조회 결과는 null이면 안됩니다.");
        assertFalse(records.isEmpty(), "Redis 스트림에 저장된 데이터가 있어야 합니다.");

        Map<Object, Object> messageMap = records.get(0).getValue();
        boolean containsPaymentId = messageMap.values().stream()
                .anyMatch(value -> value.toString().contains("testPaymentId"));
        assertTrue(containsPaymentId, "저장된 메시지에 paymentId가 포함되어 있어야 합니다.");
    }

    @Test
    @DisplayName("JSON 직렬화 실패 시 ApiException 발생 테스트")
    void testSendPaymentResultEventJsonProcessingException() throws JsonProcessingException {
        // given
        PaymentResult paymentResult = new PaymentResult(null, PaymentStatus.SUCCESS);

        ObjectMapper spyObjectMapper = spy(objectMapper);
        doThrow(new JsonProcessingException("Test serialization error") {
        }).when(spyObjectMapper).writeValueAsString(paymentResult);

        PaymentResultEventProducer paymentResultEventProducerSpy = new PaymentResultEventProducer(redisTemplate, spyObjectMapper);

        // when & then
        ApiException exception = assertThrows(ApiException.class,
                () -> paymentResultEventProducerSpy.sendPaymentResultEvent(paymentResult),
                "JSON 직렬화 오류 발생 시 ApiException이 발생해야 합니다.");
        assertEquals(GlobalErrorCode.INTERNAL_SERVER_ERROR, exception.getErrorCode(), "에러 코드가 INTERNAL_SERVER_ERROR여야 합니다.");
    }

    @Test
    @DisplayName("Redis 전송 실패 (RecordId null) 시 ApiException 발생 테스트")
    void testSendPaymentResultEventNullRecordId() {
        // given
        PaymentResult paymentResult = new PaymentResult("testPaymentId", PaymentStatus.SUCCESS);

        StringRedisTemplate mockRedisTemplate = mock(StringRedisTemplate.class);
        ObjectMapper realObjectMapper = objectMapper;

        var mockStreamOps = mock(org.springframework.data.redis.core.StreamOperations.class);
        when(mockRedisTemplate.opsForStream()).thenReturn(mockStreamOps);
        when(mockStreamOps.add(any())).thenReturn(null);

        PaymentResultEventProducer paymentResultEventProducerSpy = new PaymentResultEventProducer(mockRedisTemplate, realObjectMapper);

        // when & then
        ApiException exception = assertThrows(ApiException.class,
                () -> paymentResultEventProducerSpy.sendPaymentResultEvent(paymentResult),
                "RecordId가 null이거나 Redis가 정상적으로 동작하지 않으면 ApiException이 발생해야 합니다.");
        assertEquals(GlobalErrorCode.INTERNAL_SERVER_ERROR, exception.getErrorCode(), "에러 코드가 INTERNAL_SERVER_ERROR여야 합니다.");
    }
}
