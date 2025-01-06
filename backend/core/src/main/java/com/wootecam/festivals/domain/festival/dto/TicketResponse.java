package com.wootecam.festivals.domain.festival.dto;

import java.time.LocalDateTime;

/**
 * DTO for {@link com.wootecam.festivals.domain.ticket.entity.Ticket}
 */
public record TicketResponse(Long id,
                             String name, String detail,
                             Long price, int quantity, Long remainStock,
                             LocalDateTime startSaleTime, LocalDateTime endSaleTime,
                             LocalDateTime refundEndTime,
                             LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static TicketResponse of(com.wootecam.festivals.domain.ticket.entity.Ticket ticket) {
        return new TicketResponse(ticket.getId(),
                ticket.getName(), ticket.getDetail(),
                ticket.getPrice(), ticket.getQuantity(), (long) ticket.getQuantity(),
                ticket.getStartSaleTime(), ticket.getEndSaleTime(),
                ticket.getRefundEndTime(),
                ticket.getCreatedAt(), ticket.getUpdatedAt());
    }
}
