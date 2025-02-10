package com.wootecam.festivals.domain.payment.service;

import com.wootecam.festivals.domain.payment.entity.PaymentStatus;
import org.springframework.stereotype.Component;

@Component
public class MockExternalPaymentService implements ExternalPaymentService {

    /**
     * 외부 결제 API 호출을 시뮬레이션하는 메서드
     */
    public PaymentStatus processPayment() throws Exception {
        Thread.sleep(500);

        // 랜덤으로 결제 결과 생성 (실제 구현에서는 제거됨)
//        double random = Math.random();
//        if (random < 0.9) {
//            return PaymentStatus.SUCCESS;
//        } else {
//            return PaymentStatus.FAILED;
//        }

        return PaymentStatus.SUCCESS;
    }
}
