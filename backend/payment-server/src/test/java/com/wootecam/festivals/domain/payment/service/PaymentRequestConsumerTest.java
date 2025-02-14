package com.wootecam.festivals.domain.payment.service;

import static com.wootecam.festivals.domain.payment.constant.PaymentRedisStreamConstants.PAYMENT_REQUEST_STREAM_KEY;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wootecam.festivals.domain.payment.dto.PaymentRequest;
import com.wootecam.festivals.domain.payment.dto.PaymentResult;
import com.wootecam.festivals.domain.payment.entity.PaymentStatus;
import com.wootecam.festivals.global.exception.GlobalErrorCode;
import com.wootecam.festivals.global.exception.type.ApiException;
import com.wootecam.festivals.global.utils.RedisStreamOperator;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.core.StringRedisTemplate;

@DisplayName("PaymentRequestConsumer 테스트")
class PaymentRequestConsumerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private RedisStreamOperator redisStreamOperator;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PaymentService paymentService;

    @Mock
    private PaymentResultEventProducer paymentResultEventProducer;

    @InjectMocks
    private PaymentRequestConsumer paymentRequestConsumer;

    private final Long memberId = 1L;
    private final Long ticketId = 1L;
    private final Long ticketStockId = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("정상적인 결제 요청 메시지 처리 테스트")
    void testOnMessageSuccess() throws JsonProcessingException {
        // given
        String paymentId = "test-payment-id";
        PaymentRequest paymentRequest = new PaymentRequest(paymentId, memberId, ticketId, ticketStockId);
        ObjectRecord<String, String> message = ObjectRecord.create(PAYMENT_REQUEST_STREAM_KEY, "{\"paymentId\":\"test-payment-id\"}");

        when(objectMapper.readValue(message.getValue(), PaymentRequest.class)).thenReturn(paymentRequest);
        when(paymentService.processPayment(0, paymentId)).thenReturn(CompletableFuture.completedFuture(PaymentStatus.SUCCESS));

        // when
        assertDoesNotThrow(() -> paymentRequestConsumer.onMessage(message));

        // then
        verify(paymentService, times(1)).processPayment(0, paymentId);
        verify(paymentResultEventProducer, times(1)).sendPaymentResultEvent(new PaymentResult(paymentId, PaymentStatus.SUCCESS));
    }

    @Test
    @DisplayName("JSON 변환 예외 발생 시 예외 처리 테스트")
    void testOnMessageJsonProcessingException() throws JsonProcessingException {
        // given
        ObjectRecord<String, String> message = ObjectRecord.create(PAYMENT_REQUEST_STREAM_KEY, "invalid json");
        when(objectMapper.readValue(message.getValue(), PaymentRequest.class)).thenThrow(new JsonProcessingException("Test serialization error") {});

        // when & then
        ApiException exception = assertThrows(ApiException.class, () -> paymentRequestConsumer.onMessage(message));
        assertEquals(GlobalErrorCode.INTERNAL_SERVER_ERROR, exception.getErrorCode(), "JSON 직렬화 오류가 발생해야 합니다.");
    }

    @Test
    @DisplayName("결제 처리 실패 시 예외 처리 테스트")
    void testOnMessagePaymentProcessingFailure() throws JsonProcessingException {
        // given
        String paymentId = "test-payment-id";
        PaymentRequest paymentRequest = new PaymentRequest(paymentId, memberId, ticketId, ticketStockId);
        ObjectRecord<String, String> message = ObjectRecord.create(PAYMENT_REQUEST_STREAM_KEY, "{\"paymentId\":\"test-payment-id\"}");

        when(objectMapper.readValue(message.getValue(), PaymentRequest.class)).thenReturn(paymentRequest);
        when(paymentService.processPayment(0, paymentId)).thenThrow(new RuntimeException("Payment failed"));

        // when & then
        assertThrows(RuntimeException.class, () -> paymentRequestConsumer.onMessage(message));
    }
}
