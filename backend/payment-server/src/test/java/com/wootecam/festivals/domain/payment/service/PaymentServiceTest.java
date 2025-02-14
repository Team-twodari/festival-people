package com.wootecam.festivals.domain.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wootecam.festivals.domain.payment.entity.PaymentStatus;
import com.wootecam.festivals.domain.payment.exception.PaymentException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@DisplayName("PaymentService 테스트")
class PaymentServiceTest {

    @Mock
    private ExternalPaymentService externalPaymentService;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("결제 성공 테스트")
    void testProcessPaymentSuccess() throws Exception {
        // given
        String paymentId = "test-payment-id";
        when(externalPaymentService.processPayment()).thenReturn(PaymentStatus.SUCCESS);

        // when
        CompletableFuture<PaymentStatus> result = paymentService.processPayment(1, paymentId);

        // then
        assertEquals(PaymentStatus.SUCCESS, result.get());
        verify(externalPaymentService, times(1)).processPayment();
    }

    @Test
    @DisplayName("서버 오류로 인한 재시도 후 성공 테스트")
    void testProcessPaymentRetrySuccess() throws Exception {
        // given
        String paymentId = "test-payment-id";
        when(externalPaymentService.processPayment())
                .thenReturn(PaymentStatus.FAILED_SERVER)
                .thenReturn(PaymentStatus.SUCCESS);

        // when
        CompletableFuture<PaymentStatus> result = paymentService.processPayment(1, paymentId);

        // then
        assertEquals(PaymentStatus.SUCCESS, result.get());
        verify(externalPaymentService, times(2)).processPayment();
    }

    @Test
    @DisplayName("최대 재시도 후 실패 테스트")
    void testProcessPaymentMaxRetryFailure() throws Exception {
        // given
        String paymentId = "test-payment-id";
        when(externalPaymentService.processPayment()).thenReturn(PaymentStatus.FAILED_SERVER);

        // when
        CompletableFuture<PaymentStatus> result = paymentService.processPayment(1, paymentId);

        // then
        assertEquals(PaymentStatus.FAILED_SERVER, result.get());
        verify(externalPaymentService, times(3)).processPayment(); // 3번 재시도 후 실패
    }

    @Test
    @DisplayName("클라이언트 오류 발생 시 재시도 없이 실패")
    void testProcessPaymentClientError() throws Exception {
        // given
        String paymentId = "test-payment-id";
        when(externalPaymentService.processPayment()).thenReturn(PaymentStatus.FAILED_CLIENT);

        // when
        CompletableFuture<PaymentStatus> result = paymentService.processPayment(1, paymentId);

        // then
        assertEquals(PaymentStatus.FAILED_CLIENT, result.get());
        verify(externalPaymentService, times(1)).processPayment(); // 재시도 없이 종료
    }

    @Test
    @DisplayName("외부 서비스 예외 발생 시 PaymentException 발생")
    void testProcessPaymentThrowsException() throws Exception {
        // given
        String paymentId = "test-payment-id";
        when(externalPaymentService.processPayment()).thenThrow(new RuntimeException("Network Error"));

        // when
        CompletableFuture<PaymentStatus> result = paymentService.processPayment(1, paymentId);

        // then
        ExecutionException exception = assertThrows(ExecutionException.class, result::get);
        assertTrue(exception.getCause() instanceof PaymentException);
        verify(externalPaymentService, times(1)).processPayment();
    }
}
