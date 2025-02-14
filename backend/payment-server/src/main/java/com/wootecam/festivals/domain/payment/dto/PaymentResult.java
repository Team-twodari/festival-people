package com.wootecam.festivals.domain.payment.dto;

import com.wootecam.festivals.domain.payment.entity.PaymentStatus;

public record PaymentResult(String paymentId, PaymentStatus status) {
}
