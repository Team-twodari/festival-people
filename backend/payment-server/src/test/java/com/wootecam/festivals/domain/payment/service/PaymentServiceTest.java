//package com.wootecam.festivals.domain.payment.service;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.awaitility.Awaitility.await;
//import static org.mockito.Mockito.doThrow;
//import static org.mockito.Mockito.when;
//
//import com.wootecam.festivals.domain.payment.exception.PaymentErrorCode;
//import com.wootecam.festivals.domain.payment.service.PaymentService.PaymentStatus;
//import com.wootecam.festivals.domain.purchase.service.CompensationService;
//import com.wootecam.festivals.global.exception.type.ApiException;
//import java.util.UUID;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.TimeUnit;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("PaymentService 클래스")
//class PaymentServiceTest {
//
//    private PaymentService paymentService;
//
//    @Mock
//    CompensationService compensationService;
//
//    @Mock
//    private ExternalPaymentService externalPaymentService;
//
//    @BeforeEach
//    void setUp() {
//        paymentService = new PaymentService(compensationService, externalPaymentService);
//    }
//
//    @Nested
//    @DisplayName("initiatePayment 메소드는")
//    class Describe_initiatePayment {
//
//        @Nested
//        @DisplayName("유효한 paymentId, memberId와 ticketId가 주어졌을 때")
//        class Context_with_valid_paymentId_memberId_and_ticketId {
//
//            @Test
//            @DisplayName("CompletableFuture<PaymentStatus>를 반환한다")
//            void it_returns_completable_future_of_payment_status() throws Exception {
//                // Given
//                String paymentId = UUID.randomUUID().toString();
//                when(externalPaymentService.processPayment()).thenReturn(PaymentService.PaymentStatus.SUCCESS);
//
//                // When
//                CompletableFuture<PaymentService.PaymentStatus> future = paymentService.initiatePayment(paymentId, 1L, 1L);
//
//                // Then
//                assertThat(future).isNotNull();
//
//                await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
//                    assertThat(future.isDone()).isTrue();
//                    assertThat(future.get()).isIn(
//                            PaymentService.PaymentStatus.SUCCESS,
//                            PaymentService.PaymentStatus.FAILED
//                    );
//                });
//            }
//        }
//
//        @Nested
//        @DisplayName("externalPaymentService.processPayment 메소드가 InterruptedException을 던질 때")
//        class Context_when_externalPaymentService_processPayment_throws_InterruptedException {
//
//            @Test
//            @DisplayName("PaymentStatus.FAILED를 반환하고 캐시에 저장한다")
//            void it_should_return_failed_and_update_cache() throws Exception {
//                // Given
//                String paymentId = UUID.randomUUID().toString();
//                Long memberId = 1L;
//                Long ticketId = 1L;
//
//                // externalPaymentService.processPayment()가 InterruptedException을 던지도록 설정
//                when(externalPaymentService.processPayment()).thenThrow(new InterruptedException("테스트 인터럽트"));
//
//                // When
//                CompletableFuture<PaymentService.PaymentStatus> future = paymentService.initiatePayment(paymentId, memberId, ticketId);
//
//                // Then
//                await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
//                    assertThat(future.isDone()).isTrue();
//                    assertThat(future.get()).isEqualTo(PaymentService.PaymentStatus.FAILED);
//
//                    PaymentStatus cachedInfo = paymentService.getPaymentStatus(paymentId);
//                    assertThat(cachedInfo).isEqualTo(PaymentService.PaymentStatus.FAILED);
//                });
//            }
//        }
//
//        @Nested
//        @DisplayName("externalPaymentService.processPayment 메소드가 일반 Exception을 던질 때")
//        class Context_when_externalPaymentService_processPayment_throws_Exception {
//
//            @Test
//            @DisplayName("PaymentStatus.FAILED를 반환하고 캐시에 저장한다")
//            void it_should_return_failed_and_update_cache() throws Exception {
//                // Given
//                String paymentId = UUID.randomUUID().toString();
//                Long memberId = 1L;
//                Long ticketId = 1L;
//
//                when(externalPaymentService.processPayment()).thenThrow(new RuntimeException("테스트 런타임 예외"));
//
//                // When
//                CompletableFuture<PaymentService.PaymentStatus> future = paymentService.initiatePayment(paymentId, memberId, ticketId);
//
//                // Then
//                await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
//                    assertThat(future.isDone()).isTrue();
//                    assertThat(future.get()).isEqualTo(PaymentService.PaymentStatus.FAILED);
//
//                    PaymentStatus cachedInfo = paymentService.getPaymentStatus(paymentId);
//                    assertThat(cachedInfo).isEqualTo(PaymentService.PaymentStatus.FAILED);
//                });
//            }
//        }
//
//        @Nested
//        @DisplayName("비정상적인 paymentId가 주어졌을 때")
//        class Context_with_invalid_paymentId {
//
//            @Test
//            @DisplayName("ApiException을 던진다")
//            void it_should_throw_ApiException() {
//                // Given
//                String invalidPaymentId = "invalid-id";
//                Long memberId = 1L;
//                Long ticketId = 1L;
//
//                // When & Then
//                assertThatThrownBy(() -> paymentService.getPaymentStatus(invalidPaymentId))
//                        .isInstanceOf(ApiException.class)
//                        .hasFieldOrPropertyWithValue("errorCode", PaymentErrorCode.PAYMENT_NOT_EXIST);
//            }
//        }
//    }
//
//    @Nested
//    @DisplayName("getPaymentStatus 메소드는")
//    class Describe_getPaymentStatus {
//
//        @Nested
//        @DisplayName("존재하는 결제 ID가 주어졌을 때")
//        class Context_with_existing_payment_id {
//
//            private String paymentId;
//
//            @BeforeEach
//            void setUp() throws Exception {
//                paymentId = UUID.randomUUID().toString();
//                when(externalPaymentService.processPayment()).thenReturn(PaymentService.PaymentStatus.SUCCESS);
//                // PENDING 상태로 초기화
//                paymentService.updatePaymentStatus(paymentId, 1L, 1L, 1L, PaymentStatus.PENDING);
//
//                // CompletableFuture 완료 대기
//                paymentService.initiatePayment(paymentId, 1L, 1L).join();
//            }
//
//            @Test
//            @DisplayName("해당 결제의 상태를 반환한다")
//            void it_returns_payment_status() {
//                // When & Then
//                await().atMost(6, TimeUnit.SECONDS).untilAsserted(() -> {
//                    PaymentStatus status = paymentService.getPaymentStatus(paymentId);
//                    assertThat(status).isIn(PaymentStatus.SUCCESS, PaymentStatus.FAILED, PaymentStatus.PENDING);
//                });
//            }
//        }
//
//        @Nested
//        @DisplayName("존재하지 않는 결제 ID가 주어졌을 때")
//        class Context_with_non_existing_payment_id {
//
//            @Test
//            @DisplayName("ApiException을 던진다")
//            void it_throws_ApiException() {
//                // When & Then
//                assertThatThrownBy(() -> paymentService.getPaymentStatus("non-existing-id"))
//                        .isInstanceOf(ApiException.class)
//                        .hasFieldOrPropertyWithValue("errorCode", PaymentErrorCode.PAYMENT_NOT_EXIST);
//            }
//        }
//    }
//
//    @Nested
//    @DisplayName("updatePaymentStatus 메소드는")
//    class Describe_updatePaymentStatus {
//
//        @Test
//        @DisplayName("결제 상태를 업데이트한다")
//        void it_updates_payment_status() {
//            // Given
//            String paymentId = UUID.randomUUID().toString();
//            PaymentStatus newStatus = PaymentStatus.SUCCESS;
//
//            // When
//            paymentService.updatePaymentStatus(paymentId, 1L, 1L, 1L, newStatus);
//
//            // Then
//            assertThat(paymentService.getPaymentStatus(paymentId)).isEqualTo(newStatus);
//        }
//    }
//}