package com.wootecam.festivals.domain.payment.dto;

public record PaymentRequest(String paymentId, Long memberId, Long ticketId, Long ticketStockId) {
}
