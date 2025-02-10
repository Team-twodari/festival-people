package com.wootecam.festivals.domain.payment.service;

import com.wootecam.festivals.domain.checkin.service.CheckinService;
import com.wootecam.festivals.domain.payment.entity.Payment;
import com.wootecam.festivals.domain.payment.entity.PaymentStatus;
import com.wootecam.festivals.domain.payment.repository.PaymentRepository;
import com.wootecam.festivals.domain.purchase.entity.Purchase;
import com.wootecam.festivals.domain.purchase.service.CompensationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentResultService {

    private final CompensationService compensationService;
    private final CheckinService checkinService;
    private final PaymentRepository paymentRepository;

    /**
     * 결제 상태에 따라 Payment 상태 변경, 성공한 경우 연관된 Purchase 엔티티의 결제 상태를 변경, Checkin 엔티티를 추가합니다.
     * <p>
     * 주어진 결제 UUID를 기반으로 Payment 엔티티를 조회한 후, 해당 Payment와 연관된 Purchase 엔티티를 가져옵니다.
     * 이후 {@code status}에 따라 성공(SUCCESS)인 경우 결제 성공 처리, 실패(FAILED_SERVER, FAILED_CLIENT)인 경우 결제 실패 처리 메서드를 호출합니다.
     * 결제 성공인 경우 CheckinService를 통해 Checkin 엔티티를 생성합니다.
     * </p>
     *
     * @param paymentId 결제 UUID (비즈니스 식별자)
     * @param status    결제 상태 (예: SUCCESS, FAILED_SERVER, FAILED_CLIENT)
     * @throws RuntimeException 결제 정보를 찾지 못한 경우 예외 발생
     */
    @Transactional
    public void handlePaymentStatus(String paymentId, PaymentStatus status) {
        Payment payment = paymentRepository.findByPaymentUuidWithPurchase(paymentId)
                .orElseThrow(() -> {
                    log.error("결제 정보를 찾을 수 없습니다. paymentId: {}", paymentId);
                    return new RuntimeException("결제 정보를 찾을 수 없습니다. paymentId: " + paymentId);
                });
        Purchase purchase = payment.getPurchase();

        switch (status) {
            case SUCCESS -> processPaymentSuccess(payment, purchase);
            case FAILED_SERVER, FAILED_CLIENT -> processPaymentFail(payment, status, purchase);
        }
    }

    /**
     * 결제 성공 처리를 수행합니다.
     * <p>
     * 이 메서드는 Payment 엔티티의 {@code success()} 메서드를 호출하여 결제 상태를 성공(SUCCESS)으로 변경한 후,
     * 연관된 Purchase 엔티티의 멤버와 티켓 정보를 이용해 대기 중인(check-in pending) 체크인을 생성합니다.
     * </p>
     *
     * @param payment  결제 정보를 가진 Payment 엔티티. 이 객체에서 결제 상태가 변경됩니다.
     * @param purchase 결제에 연관된 Purchase 엔티티. 체크인 생성 시 멤버와 티켓 정보를 제공하기 위해 사용됩니다.
     */
    private void processPaymentSuccess(Payment payment, Purchase purchase) {
        payment.success();
        checkinService.createPendingCheckin(purchase.getMember().getId(), purchase.getTicket().getId());
    }

    /**
     * 결제 실패 처리를 수행합니다.
     * <p>
     * 이 메서드는 결제 실패에 따른 보상(compensation) 처리를 위해 {@code compensateFailedPurchase()}를 호출하고,
     * Payment 엔티티의 {@code fail(status)} 메서드를 호출하여 결제 상태를 실패(FAILED_SERVER, FAILED_CLIENT)로 변경합니다.
     * </p>
     *
     * @param payment  결제 정보를 가진 Payment 엔티티. 이 객체에서 결제 상태가 변경됩니다.
     * @param status   적용할 결제 실패 상태 (예: FAILED_SERVER, FAILED_CLIENT).
     * @param purchase 결제에 연관된 Purchase 엔티티. 보상 처리를 위한 체크인 생성 시 티켓 및 멤버 정보를 제공하기 위해 사용됩니다.
     */
    private void processPaymentFail(Payment payment, PaymentStatus status, Purchase purchase) {
        compensationService.compensateFailedPurchase(payment.getPaymentUuid(),
                purchase.getTicket().getId(),
                purchase.getMember().getId());
        payment.fail(status);
    }
}
