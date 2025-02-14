package com.wootecam.festivals.domain.purchase.service;

import static com.wootecam.festivals.domain.purchase.exception.PurchaseErrorCode.PURCHASE_NOT_FOUND;

import com.wootecam.festivals.domain.member.repository.MemberRepository;
import com.wootecam.festivals.domain.payment.dto.PaymentRequest;
import com.wootecam.festivals.domain.payment.entity.Payment;
import com.wootecam.festivals.domain.payment.entity.PaymentStatus;
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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseFacadeService {

    private final TicketCacheService ticketCacheService;
    private final TimeProvider timeProvider;

    private final PaymentRequestEventProducer paymentRequestEventProducer;
    private final PurchaseRepository purchaseRepository;
    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;

    /**
     * 구매 요청을 처리합니다.
     * @param purchaseData 구매 요청 데이터
     * @return 결제 ID
     * @throws ApiException 구매가 불가능한 경우 예외 발생
     */
    @Transactional
    public String processPurchase(PurchaseData purchaseData) {
        validatePurchase(purchaseData);

        String paymentId = UUID.randomUUID().toString();
        Purchase purchase = createInitialPurchase(purchaseData, paymentId);
        Purchase savedPurchase = purchaseRepository.save(purchase);

        Payment payment = createInitialPayment(paymentId, savedPurchase);
        paymentRepository.save(payment);

        paymentRequestEventProducer.sendPaymentEvent(new PaymentRequest(
                paymentId, purchaseData.memberId(), purchaseData.ticketId(), purchaseData.ticketStockId()));

        return paymentId;
    }

    private Payment createInitialPayment(String paymentUuid, Purchase purchase) {
        return Payment.builder()
                .paymentUuid(paymentUuid)
                .paymentTime(timeProvider.getCurrentTime())
                .paymentStatus(PaymentStatus.INITIATED)
                .purchase(purchase)
                .build();
    }

    private Purchase createInitialPurchase(PurchaseData purchaseData, String paymentUuid) {
        return Purchase.builder()
                .paymentUuid(paymentUuid)
                .ticket(ticketCacheService.getTicket(purchaseData.ticketId()))
                .member(memberRepository.getReferenceById(purchaseData.memberId()))
                .purchaseTime(timeProvider.getCurrentTime())
                .purchaseStatus(PurchaseStatus.INITIATED)
                .build();
    }

    private void validatePurchase(PurchaseData purchaseData) {
        if (!isTicketPurchasableTime(purchaseData.ticketId())) {
            throw new ApiException(PurchaseErrorCode.INVALID_TICKET_PURCHASE_TIME);
        }
    }

    private boolean isTicketPurchasableTime(Long ticketId) {
        Ticket ticket = ticketCacheService.getTicket(ticketId); // 없다면 내부에서 db조회 후 가져온다.

        LocalDateTime now = timeProvider.getCurrentTime();
        return now.isAfter(ticket.getStartSaleTime()) && now.isBefore(ticket.getEndSaleTime());
    }

    /**
     * 결제 ID의 현재 결제 상태를 조회합니다.
     * @param paymentId 결제 ID
     */
    public PurchaseStatus getPaymentStatus(String paymentId) {
        Purchase purchase = purchaseRepository.findByPaymentUuid(paymentId)
                .orElseThrow(() -> new ApiException(PURCHASE_NOT_FOUND));

        return purchase.getPurchaseStatus();
    }
}
