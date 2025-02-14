package com.wootecam.festivals.domain.purchase.service;

import static com.wootecam.festivals.utils.Fixture.createFestival;
import static com.wootecam.festivals.utils.Fixture.createMember;
import static com.wootecam.festivals.utils.Fixture.createTicket;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wootecam.festivals.domain.festival.entity.Festival;
import com.wootecam.festivals.domain.member.entity.Member;
import com.wootecam.festivals.domain.member.repository.MemberRepository;
import com.wootecam.festivals.domain.payment.dto.PaymentRequest;
import com.wootecam.festivals.domain.payment.repository.PaymentRepository;
import com.wootecam.festivals.domain.payment.service.PaymentRequestEventProducer;
import com.wootecam.festivals.domain.purchase.dto.PurchaseData;
import com.wootecam.festivals.domain.purchase.entity.Purchase;
import com.wootecam.festivals.domain.purchase.entity.PurchaseStatus;
import com.wootecam.festivals.domain.purchase.exception.PurchaseErrorCode;
import com.wootecam.festivals.domain.purchase.repository.PurchaseRepository;
import com.wootecam.festivals.domain.ticket.entity.Ticket;
import com.wootecam.festivals.domain.ticket.service.TicketCacheService;
import com.wootecam.festivals.global.exception.type.ApiException;
import com.wootecam.festivals.global.utils.TimeProvider;
import java.time.LocalDateTime;
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
class PurchaseFacadeServiceTest {

    private final Long memberId = 1L;
    private final Long ticketId = 1L;
    private final Long ticketStockId = 1L;

    @Mock
    private TicketCacheService ticketCacheService;

    @Mock
    private TimeProvider timeProvider;

    @Mock
    private PaymentRequestEventProducer paymentRequestEventProducer;

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private PurchaseFacadeService purchaseFacadeService;

    @Nested
    @DisplayName("processPurchase 메소드는")
    class Describe_processPurchase {
        private Member member = createMember("test", "email");
        private Festival festival = createFestival(member, "title", "description",
                LocalDateTime.now().plusDays(7), LocalDateTime.now().plusDays(14));
        private Ticket ticket = createTicket(festival, 1000L, 10,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        @Nested
        @DisplayName("유효한 구매 요청이 주어졌을 때")
        class Context_with_valid_purchase_request {
            private LocalDateTime fixedTime;
            private Ticket ticket;

            @BeforeEach
            void setUp() {
                // 현재 시간을 고정
                fixedTime = LocalDateTime.now();
                when(timeProvider.getCurrentTime()).thenReturn(fixedTime);

                // 티켓의 판매 시작/종료 시간이 현재 시간을 포함하도록 설정
                ticket = mock(Ticket.class);
                when(ticket.getStartSaleTime()).thenReturn(fixedTime.minusDays(1));
                when(ticket.getEndSaleTime()).thenReturn(fixedTime.plusDays(1));
                when(ticketCacheService.getTicket(ticketId)).thenReturn(ticket);

                // 회원 정보는 단순 더미 객체로 처리 (memberRepository.getReferenceById 내부에서만 사용)
                when(memberRepository.getReferenceById(memberId)).thenReturn(member);

                // purchaseRepository와 paymentRepository는 save 시 전달된 객체를 그대로 반환하도록 설정
                when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> invocation.getArgument(0));
                when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            }

            @Test
            @DisplayName("결제 ID를 반환하고, 관련 저장 및 이벤트 호출이 이루어진다")
            void it_returns_payment_id_and_calls_dependencies() {
                // given
                PurchaseData purchaseData = new PurchaseData(memberId, ticketId, ticketStockId);

                // when
                String paymentId = purchaseFacadeService.processPurchase(purchaseData);

                // then
                assertThat(paymentId).isNotNull().isNotEmpty();
                verify(purchaseRepository).save(any(Purchase.class));
                verify(paymentRepository).save(any());
                verify(paymentRequestEventProducer).sendPaymentEvent(any(PaymentRequest.class));
            }
        }

        @Nested
        @DisplayName("유효하지 않은 구매 시간일 때")
        class Context_with_invalid_purchase_time {
            private LocalDateTime fixedTime;
            private Ticket ticket;

            @BeforeEach
            void setUp() {
                ticket = mock(Ticket.class);
                fixedTime = LocalDateTime.now();
                when(timeProvider.getCurrentTime()).thenReturn(fixedTime);
            }

            @Test
            @DisplayName("판매 시작 시각이 미래라면 ApiException 예외를 던진다")
            void it_throws_exception() {
                // given
                when(ticket.getStartSaleTime()).thenReturn(fixedTime.plusDays(1));
                when(ticketCacheService.getTicket(ticketId)).thenReturn(ticket);

                PurchaseData purchaseData = new PurchaseData(memberId, ticketId, ticketStockId);

                // when, then
                ApiException exception = assertThrows(ApiException.class, () ->
                        purchaseFacadeService.processPurchase(purchaseData));
                assertThat(exception.getErrorCode()).isEqualTo(PurchaseErrorCode.INVALID_TICKET_PURCHASE_TIME);
            }
        }
    }

    @Nested
    @DisplayName("getPaymentStatus 메소드는")
    class Describe_getPaymentStatus {

        @Nested
        @DisplayName("유효한 결제 ID가 주어졌을 때")
        class Context_with_valid_payment_id {

            @BeforeEach
            void setUp() {
                // PurchaseStatus가 INITIATED인 Purchase 객체 반환하도록 설정
                Ticket ticket = mock(Ticket.class);
                Member member = mock(Member.class);
                LocalDateTime purchaseTime = LocalDateTime.now();

                Purchase purchase = Purchase.builder()
                        .paymentUuid("payment-123")
                        .ticket(ticket)
                        .member(member)
                        .purchaseTime(purchaseTime)
                        .purchaseStatus(PurchaseStatus.INITIATED)
                        .build();
                when(purchaseRepository.findByPaymentUuid("payment-123")).thenReturn(Optional.of(purchase));
            }

            @Test
            @DisplayName("해당 결제 상태를 반환한다")
            void it_returns_payment_status() {
                PurchaseStatus status = purchaseFacadeService.getPaymentStatus("payment-123");
                assertThat(status).isEqualTo(PurchaseStatus.INITIATED);
            }
        }

        @Nested
        @DisplayName("유효하지 않은 결제 ID가 주어졌을 때")
        class Context_with_invalid_payment_id {

            @BeforeEach
            void setUp() {
                when(purchaseRepository.findByPaymentUuid("invalid-id")).thenReturn(Optional.empty());
            }

            @Test
            @DisplayName("ApiException 예외를 던진다")
            void it_throws_exception() {
                ApiException exception = assertThrows(ApiException.class, () ->
                        purchaseFacadeService.getPaymentStatus("invalid-id"));
                assertThat(exception.getErrorCode()).isEqualTo(PurchaseErrorCode.PURCHASE_NOT_FOUND);
            }
        }
    }
}
