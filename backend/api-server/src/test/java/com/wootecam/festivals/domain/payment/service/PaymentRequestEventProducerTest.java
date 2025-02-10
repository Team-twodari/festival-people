package com.wootecam.festivals.domain.payment.service;

import static com.wootecam.festivals.domain.payment.constant.PaymentRedisStreamConstants.PAYMENT_REQUEST_STREAM_KEY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wootecam.festivals.domain.payment.dto.PaymentRequest;
import com.wootecam.festivals.global.exception.GlobalErrorCode;
import com.wootecam.festivals.global.exception.type.ApiException;
import com.wootecam.festivals.utils.SpringBootTestConfig;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;

@DisplayName("PaymentRequestEventProducer 통합 테스트")
class PaymentRequestEventProducerTest extends SpringBootTestConfig {

    @Autowired
    private PaymentRequestEventProducer eventProducer;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 각 테스트 실행 전에 Redis의 해당 스트림 데이터를 초기화합니다.
     */
    @BeforeEach
    void setUp() {
        // PAYMENT_REQUEST_STREAM_KEY에 해당하는 Redis 스트림 데이터를 삭제하여 초기화합니다.
        redisTemplate.delete(PAYMENT_REQUEST_STREAM_KEY);
    }

    /**
     * 정상적인 payment event 전송 시, Redis 스트림에 메시지가 저장되는지 검증합니다.
     *
     * <p>테스트 시나리오:
     * <ul>
     *   <li>테스트용 PaymentRequest 객체를 생성합니다.</li>
     *   <li>sendPaymentEvent 메서드 호출 후 Redis 스트림에서 저장된 메시지를 조회합니다.</li>
     *   <li>저장된 메시지에 paymentId가 포함되어 있는지 확인합니다.</li>
     * </ul>
     * </p>
     */
    @Test
    @DisplayName("정상적인 payment event 전송 테스트")
    void testSendPaymentEventSuccess() {
        // given: 테스트용 PaymentRequest 객체 생성 (paymentId가 "testPaymentId")
        PaymentRequest paymentRequest = new PaymentRequest("testPaymentId", 1L, 1L, 1L);

        // when: payment event 전송
        eventProducer.sendPaymentEvent(paymentRequest);

        // then: Redis 스트림에 메시지가 저장되었는지 검증
        List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream()
                .range(PAYMENT_REQUEST_STREAM_KEY, Range.unbounded());
        assertNotNull(records, "Redis 스트림 조회 결과는 null이면 안됩니다.");
        assertFalse(records.isEmpty(), "Redis 스트림에 저장된 데이터가 있어야 합니다.");

        // 저장된 메시지 중 하나의 값에 paymentId("testPaymentId")가 포함되어 있는지 확인
        Map<Object, Object> messageMap = records.get(0).getValue();
        boolean containsPaymentId = messageMap.values().stream()
                .anyMatch(value -> value.toString().contains("testPaymentId"));
        assertTrue(containsPaymentId, "저장된 메시지에 paymentId가 포함되어 있어야 합니다.");
    }

    /**
     * ObjectMapper의 직렬화 과정에서 JsonProcessingException 발생 시,
     * ApiException이 발생하는지 검증합니다.
     *
     * <p>테스트 시나리오:
     * <ul>
     *   <li>ObjectMapper를 스파이하여 writeValueAsString 호출 시 예외를 강제합니다.</li>
     *   <li>새로운 PaymentRequestEventProducer 인스턴스를 생성하고 메서드를 호출합니다.</li>
     *   <li>ApiException이 발생하며, 에러 코드가 INTERNAL_SERVER_ERROR 인지 확인합니다.</li>
     * </ul>
     * </p>
     */
    @Test
    @DisplayName("JSON 직렬화 실패 시 ApiException 발생 테스트")
    void testSendPaymentEventJsonProcessingException() throws JsonProcessingException {
        // given: 테스트용 PaymentRequest 객체 생성
        PaymentRequest paymentRequest = new PaymentRequest("testPaymentId", 1L, 1L, 1L);

        // ObjectMapper를 스파이하고 writeValueAsString 호출 시 JsonProcessingException을 발생하도록 설정
        ObjectMapper spyObjectMapper = spy(objectMapper);
        doThrow(new JsonProcessingException("Test serialization error") {
        }).when(spyObjectMapper).writeValueAsString(paymentRequest);

        // 스파이한 ObjectMapper를 사용하여 새로운 PaymentRequestEventProducer 생성
        PaymentRequestEventProducer eventProducerWithSpy = new PaymentRequestEventProducer(redisTemplate, spyObjectMapper);

        // when & then: sendPaymentEvent 호출 시 ApiException이 발생하는지 검증
        ApiException exception = assertThrows(ApiException.class,
                () -> eventProducerWithSpy.sendPaymentEvent(paymentRequest),
                "직렬화 오류 발생 시 ApiException이 발생해야 합니다.");
        assertEquals(GlobalErrorCode.INTERNAL_SERVER_ERROR, exception.getErrorCode(),
                "에러 코드가 INTERNAL_SERVER_ERROR여야 합니다.");
    }

    /**
     * Redis에 메시지 전송 후 반환된 RecordId가 null인 경우 ApiException이 발생하는지 검증합니다.
     *
     * <p>테스트 시나리오:
     * <ul>
     *   <li>StringRedisTemplate과 해당 스트림 연산(StreamOperations)을 목(mock) 처리하여 add() 호출 시 null을 반환하도록 설정합니다.</li>
     *   <li>새로운 PaymentRequestEventProducer 인스턴스를 생성하고 메서드를 호출합니다.</li>
     *   <li>ApiException이 발생하며, 에러 코드가 INTERNAL_SERVER_ERROR 인지 확인합니다.</li>
     * </ul>
     * </p>
     */
    @Test
    @DisplayName("Redis 전송 실패 (RecordId null) 시 ApiException 발생 테스트")
    void testSendPaymentEventNullRecordId() throws JsonProcessingException {
        // given: 테스트용 PaymentRequest 객체 생성
        PaymentRequest paymentRequest = new PaymentRequest("testPaymentId", 1L, 1L, 1L);

        // StringRedisTemplate과 StreamOperations를 목(mock) 처리
        StringRedisTemplate mockRedisTemplate = mock(StringRedisTemplate.class);
        // 실제 ObjectMapper 사용
        ObjectMapper realObjectMapper = objectMapper;
        // StreamOperations 목(mock) 생성 후, add() 호출 시 null 반환하도록 설정
        var mockStreamOps = mock(org.springframework.data.redis.core.StreamOperations.class);
        when(mockRedisTemplate.opsForStream()).thenReturn(mockStreamOps);
        when(mockStreamOps.add(any())).thenReturn(null);

        // 목 처리한 redisTemplate을 사용하여 새로운 PaymentRequestEventProducer 생성
        PaymentRequestEventProducer producerWithNullRecord = new PaymentRequestEventProducer(mockRedisTemplate, realObjectMapper);

        // when & then: sendPaymentEvent 호출 시 ApiException이 발생하는지 검증
        ApiException exception = assertThrows(ApiException.class,
                () -> producerWithNullRecord.sendPaymentEvent(paymentRequest),
                "RecordId가 null인 경우 ApiException이 발생해야 합니다.");
        assertEquals(GlobalErrorCode.INTERNAL_SERVER_ERROR, exception.getErrorCode(),
                "에러 코드가 INTERNAL_SERVER_ERROR여야 합니다.");
    }
}
