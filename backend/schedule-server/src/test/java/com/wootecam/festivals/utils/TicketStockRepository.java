package com.wootecam.festivals.utils;

import com.wootecam.festivals.domain.ticket.entity.TicketStock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketStockRepository extends JpaRepository<TicketStock, Long> {
}
