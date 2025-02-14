package com.wootecam.festivals.domain.payment.service;

import com.wootecam.festivals.domain.payment.entity.PaymentStatus;

public interface ExternalPaymentService {

    PaymentStatus processPayment() throws Exception;
}
