package com.wootecam.festivals.domain.purchase.dto;

import com.wootecam.festivals.domain.purchase.entity.PurchaseStatus;

public record PaymentStatusResponse(PurchaseStatus paymentStatus) {
}
