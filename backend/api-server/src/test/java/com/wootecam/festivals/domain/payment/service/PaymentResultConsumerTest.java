package com.wootecam.festivals.domain.payment.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wootecam.festivals.domain.payment.dto.PaymentResult;
import com.wootecam.festivals.domain.payment.entity.PaymentStatus;
import com.wootecam.festivals.global.exception.GlobalErrorCode;
import com.wootecam.festivals.global.exception.type.ApiException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.core.StringRedisTemplate;

@DisplayName("PaymentResultConsumer 테스트")
class PaymentResultConsumerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PaymentResultService paymentResultService;

    @InjectMocks
    private PaymentResultConsumer paymentResultConsumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("정상적인 메시지 처리 테스트")
    void testOnMessageSuccess() throws JsonProcessingException {
        // given
        String paymentId = UUID.randomUUID().toString();
        PaymentResult paymentResult = new PaymentResult(paymentId, PaymentStatus.SUCCESS);
        String serializedMessage = "{\"paymentId\":\"" + paymentId + "\",\"status\":\"SUCCESS\"}";
        ObjectRecord<String, String> message = ObjectRecord.create("stream-key", serializedMessage);

        when(objectMapper.readValue(serializedMessage, PaymentResult.class)).thenReturn(paymentResult);

        // when
        assertDoesNotThrow(() -> paymentResultConsumer.onMessage(message));

        // then
        verify(paymentResultService, times(1)).handlePaymentStatus(paymentId, PaymentStatus.SUCCESS);
    }

    @Test
    @DisplayName("JSON 변환 예외 발생 시 ApiException 테스트")
    void testOnMessageJsonProcessingException() throws JsonProcessingException {
        // given
        String invalidMessage = "invalid json";
        ObjectRecord<String, String> message = ObjectRecord.create("stream-key", invalidMessage);

        when(objectMapper.readValue(invalidMessage, PaymentResult.class)).thenThrow(new JsonProcessingException("JSON 오류") {});

        // when & then
        ApiException exception = assertThrows(ApiException.class, () -> paymentResultConsumer.onMessage(message));
        assertEquals(GlobalErrorCode.INTERNAL_SERVER_ERROR, exception.getErrorCode(), "에러 코드가 INTERNAL_SERVER_ERROR여야 합니다.");
    }

    @Test
    @DisplayName("RuntimeException 발생 시 ApiException 테스트")
    void testOnMessageRuntimeException() throws JsonProcessingException {
        // given
        String paymentId = UUID.randomUUID().toString();
        PaymentResult paymentResult = new PaymentResult(paymentId, PaymentStatus.FAILED_SERVER);
        String serializedMessage = "{\"paymentId\":\"" + paymentId + "\",\"status\":\"FAILED\"}";
        ObjectRecord<String, String> message = ObjectRecord.create("stream-key", serializedMessage);

        when(objectMapper.readValue(serializedMessage, PaymentResult.class)).thenReturn(paymentResult);
        doThrow(new RuntimeException("Service exception"))
                .when(paymentResultService).handlePaymentStatus(paymentId, PaymentStatus.FAILED_SERVER);

        // when & then
        ApiException exception = assertThrows(ApiException.class, () -> paymentResultConsumer.onMessage(message));
        assertEquals(GlobalErrorCode.INTERNAL_SERVER_ERROR, exception.getErrorCode(), "에러 코드가 INTERNAL_SERVER_ERROR여야 합니다.");
    }
}
