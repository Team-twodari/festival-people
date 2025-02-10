package com.wootecam.festivals.domain.ticket.entity;

import static com.wootecam.festivals.domain.ticket.entity.TicketValidator.validTicket;

import com.wootecam.festivals.domain.festival.entity.Festival;
import com.wootecam.festivals.domain.member.entity.Member;
import com.wootecam.festivals.domain.purchase.entity.Purchase;
import com.wootecam.festivals.domain.purchase.entity.PurchaseStatus;
import com.wootecam.festivals.global.audit.BaseEntity;
import com.wootecam.festivals.global.utils.DateTimeUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ticket")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Ticket extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "festival_id", nullable = false, updatable = false)
    private Festival festival;

    @Column(name = "ticket_name", nullable = false)
    private String name;

    @Column(name = "ticket_detail", nullable = false)
    private String detail;

    @Column(name = "ticket_price", nullable = false)
    private Long price;

    @Column(name = "ticket_quantity", nullable = false)
    private int quantity;

    @Column(name = "start_sale_time", nullable = false)
    private LocalDateTime startSaleTime;

    @Column(name = "end_sale_time", nullable = false)
    private LocalDateTime endSaleTime;

    @Column(name = "end_refund_time", nullable = false)
    private LocalDateTime refundEndTime;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @Builder
    private Ticket(Festival festival,
                   String name, String detail, Long price, int quantity,
                   LocalDateTime startSaleTime, LocalDateTime endSaleTime, LocalDateTime refundEndTime) {
        validTicket(festival, name, detail, price, quantity, startSaleTime, endSaleTime, refundEndTime);
        this.festival = festival;
        this.name = name;
        this.detail = detail;
        this.price = price;
        this.quantity = quantity;
        this.startSaleTime = DateTimeUtils.normalizeDateTime(startSaleTime);
        this.endSaleTime = DateTimeUtils.normalizeDateTime(endSaleTime);
        this.refundEndTime = DateTimeUtils.normalizeDateTime(refundEndTime);
        this.isDeleted = false;
    }

    public List<TicketStock> createTicketStock() {
        List<TicketStock> ticketStocks = new ArrayList<>();

        for (int i = 0; i < quantity; i++) {
            ticketStocks.add(TicketStock.builder()
                    .ticket(this)
                    .build());
        }

        return Collections.unmodifiableList(ticketStocks);
    }

    public Purchase createPurchase(Member member) {
        return Purchase.builder()
                .ticket(this)
                .member(member)
                .purchaseTime(LocalDateTime.now())
                .purchaseStatus(PurchaseStatus.INITIATED)
                .build();
    }

    public boolean isSaleOnTime(LocalDateTime now) {
        return (startSaleTime.isEqual(now) || startSaleTime.isBefore(now)) && (endSaleTime.isEqual(now)
                || endSaleTime.isAfter(now));
    }
}
