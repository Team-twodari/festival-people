package com.wootecam.festivals.domain.payment.entity;

public enum PaymentStatus {
    INITIATED, // 결제 시도 전
    IN_PROGRESS, // 결제 시도 중
    SUCCESS, // 결제 성공
    FAILED_CLIENT, // 잔액 부족 등의 사용자 과실로 인한 결제 실패
    FAILED_SERVER; // 네트워크, 외부 결제 서버의 단기 장애 등의 외부 요소 과실로 인한 결제 실패

    public boolean isSuccess() {
        return this == SUCCESS;
    }

    public boolean isFailed() {
        return this == FAILED_CLIENT || this == FAILED_SERVER;
    }

    public boolean isFailedByServer() {
        return this == FAILED_SERVER;
    }
}
