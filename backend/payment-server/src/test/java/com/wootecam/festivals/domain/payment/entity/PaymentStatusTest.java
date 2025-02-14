package com.wootecam.festivals.domain.payment.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PaymentStatus 테스트")
class PaymentStatusTest {

    @Test
    @DisplayName("실패 상태 확인 테스트")
    void testIsFailed() {
        assertTrue(PaymentStatus.FAILED_CLIENT.isFailed(), "FAILED_CLIENT 상태는 isFailed()가 true여야 합니다.");
        assertTrue(PaymentStatus.FAILED_SERVER.isFailed(), "FAILED_SERVER 상태는 isFailed()가 true여야 합니다.");
        assertFalse(PaymentStatus.SUCCESS.isFailed(), "SUCCESS 상태는 isFailed()가 false여야 합니다.");
        assertFalse(PaymentStatus.INITIATED.isFailed(), "INITIATED 상태는 isFailed()가 false여야 합니다.");
        assertFalse(PaymentStatus.IN_PROGRESS.isFailed(), "IN_PROGRESS 상태는 isFailed()가 false여야 합니다.");
    }

    @Test
    @DisplayName("서버 원인 실패 확인 테스트")
    void testIsFailedByServer() {
        assertTrue(PaymentStatus.FAILED_SERVER.isFailedByServer(), "FAILED_SERVER 상태는 isFailedByServer()가 true여야 합니다.");
        assertFalse(PaymentStatus.FAILED_CLIENT.isFailedByServer(), "FAILED_CLIENT 상태는 isFailedByServer()가 false여야 합니다.");
        assertFalse(PaymentStatus.SUCCESS.isFailedByServer(), "SUCCESS 상태는 isFailedByServer()가 false여야 합니다.");
        assertFalse(PaymentStatus.INITIATED.isFailedByServer(), "INITIATED 상태는 isFailedByServer()가 false여야 합니다.");
        assertFalse(PaymentStatus.IN_PROGRESS.isFailedByServer(), "IN_PROGRESS 상태는 isFailedByServer()가 false여야 합니다.");
    }
}
