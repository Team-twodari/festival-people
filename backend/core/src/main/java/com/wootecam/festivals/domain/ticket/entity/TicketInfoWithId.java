package com.wootecam.festivals.domain.ticket.entity;

import java.time.LocalDateTime;

public record TicketInfoWithId(Long ticketId,
                               LocalDateTime startSaleTime,
                               LocalDateTime endSaleTime) {
}
