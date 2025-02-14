package com.wootecam.festivals.domain.payment.entity;

import com.wootecam.festivals.domain.purchase.entity.Purchase;
import com.wootecam.festivals.global.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

     // 사용자가 결제 상태를 확인할 수 있는 결제 uuid 정보
    @Column(name = "payment_uuid", nullable = false, unique = true, updatable = false)
    private String paymentUuid;

    @Column(name = "payment_time", nullable = false)
    private LocalDateTime paymentTime;

    @Column(name = "payment_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id", referencedColumnName = "purchase_id")
    private Purchase purchase;

    @Builder
    private Payment(Long id,
                    String paymentUuid,
                    LocalDateTime paymentTime,
                    PaymentStatus paymentStatus,
                    Purchase purchase) {
        this.paymentUuid = Objects.requireNonNull(paymentUuid, "결제 uuid 정보는 필수입니다.");
        this.paymentTime = Objects.requireNonNull(paymentTime, "결제 시각은 필수입니다.");
        this.paymentStatus = Objects.requireNonNull(paymentStatus, "결제 상태는 필수입니다.");
        this.purchase = Objects.requireNonNull(purchase, "구매 내역은 필수입니다.");
    }

    public void success() {
        this.paymentStatus = PaymentStatus.SUCCESS;
        this.purchase.paid();
    }

    public void fail(PaymentStatus status) {
        if(!status.isFailed()) {
            throw new IllegalArgumentException("결제 실패 상태여야 합니다. " + status.name());
        }
        this.paymentStatus = status;
    }
}
