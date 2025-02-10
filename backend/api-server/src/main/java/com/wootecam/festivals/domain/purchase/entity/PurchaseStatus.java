package com.wootecam.festivals.domain.purchase.entity;

import com.wootecam.festivals.global.docs.EnumType;


public enum PurchaseStatus implements EnumType {

    /**
     * 구매를 시도하여 결제를 진행하기 전 상태(장바구니 개념)
     */
    INITIATED("구매 준비"),

    /**
     * 결제가 정상적으로 완료된 상태
     */
    PAID("결제 완료"),

    /**
     * 구매 취소가 진행된 상태(결제 취소, 환불 등)
     */
    CANCELED("구매 취소"),

    /**
     * 환불이 완료된 상태
     */
    REFUNDED("환불 완료");

    private final String description;

    PurchaseStatus(String description) {
        this.description = description;
    }

    public boolean isPurchased() {
        return this == PAID;
    }

    @Override
    public String getName() {
        return name();
    }

    @Override
    public String getDescription() {
        return description;
    }
}
