package com.wootecam.festivals.domain.payment.repository;

import com.wootecam.festivals.domain.payment.entity.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT p FROM Payment p JOIN FETCH p.purchase WHERE p.paymentUuid = :paymentUuid ")
    Optional<Payment> findByPaymentUuidWithPurchase(@Param("paymentUuid") String paymentUuid);
}