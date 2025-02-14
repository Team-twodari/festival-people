package com.wootecam.festivals.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.wootecam.festivals.domain.checkin.service.CheckinService;
import com.wootecam.festivals.domain.member.entity.Member;
import com.wootecam.festivals.domain.payment.entity.Payment;
import com.wootecam.festivals.domain.payment.entity.PaymentStatus;
import com.wootecam.festivals.domain.payment.repository.PaymentRepository;
import com.wootecam.festivals.domain.purchase.entity.Purchase;
import com.wootecam.festivals.domain.purchase.service.CompensationService;
import com.wootecam.festivals.domain.ticket.entity.Ticket;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentResultServiceTest {

    @Mock
    private CompensationService compensationService;

    @Mock
    private CheckinService checkinService;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentResultService paymentResultService;

    private final String paymentUuid = "test-payment-123";
    private Payment payment;
    private Purchase purchase;

    @BeforeEach
    void setUp() {
        payment = mock(Payment.class);
        purchase = mock(Purchase.class);
    }

    @Nested
    @DisplayName("handlePaymentStatus 메소드는")
    class Describe_handlePaymentStatus {

        @Nested
        @DisplayName("유효한 결제 ID가 주어졌을 때")
        class Context_with_valid_payment_id {

            private Ticket ticket = mock(Ticket.class);
            private Member member = mock(Member.class);

            @BeforeEach
            void setUp() {
                when(payment.getPurchase()).thenReturn(purchase);
                when(paymentRepository.findByPaymentUuidWithPurchase(paymentUuid)).thenReturn(Optional.of(payment));

                when(purchase.getMember()).thenReturn(member);
                when(purchase.getTicket()).thenReturn(ticket);
            }

            @Test
            @DisplayName("결제 성공 상태면 processPaymentSuccess를 호출한다")
            void it_calls_processPaymentSuccess() {
                // when
                paymentResultService.handlePaymentStatus(paymentUuid, PaymentStatus.SUCCESS);
                // then
                verify(payment).success();
                verify(checkinService).createPendingCheckin(anyLong(), anyLong());
            }

            @Test
            @DisplayName("결제 실패 상태면 processPaymentFail을 호출한다")
            void it_calls_processPaymentFail() {
                // given
                when(payment.getPaymentUuid()).thenReturn(paymentUuid);

                when(ticket.getId()).thenReturn(1L);
                when(member.getId()).thenReturn(1L);

                // when
                paymentResultService.handlePaymentStatus(paymentUuid, PaymentStatus.FAILED_SERVER);
                // then
                verify(compensationService).compensateFailedPurchase(anyString(), anyLong(), anyLong());
                verify(payment).fail(PaymentStatus.FAILED_SERVER);
            }
        }

        @Nested
        @DisplayName("유효하지 않은 결제 ID가 주어졌을 때")
        class Context_with_invalid_payment_id {

            @BeforeEach
            void setUp() {
                when(paymentRepository.findByPaymentUuidWithPurchase("invalid-id"))
                        .thenReturn(Optional.empty());
            }

            @Test
            @DisplayName("예외를 던진다")
            void it_throws_exception() {
                RuntimeException exception = assertThrows(RuntimeException.class, () ->
                        paymentResultService.handlePaymentStatus("invalid-id", PaymentStatus.SUCCESS));
                assertThat(exception.getMessage()).contains("결제 정보를 찾을 수 없습니다");
            }
        }
    }
}
