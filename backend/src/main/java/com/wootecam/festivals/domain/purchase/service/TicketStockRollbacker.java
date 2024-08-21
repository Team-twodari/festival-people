package com.wootecam.festivals.domain.purchase.service;


import com.wootecam.festivals.domain.ticket.entity.TicketStock;
import com.wootecam.festivals.domain.ticket.repository.TicketRepository;
import com.wootecam.festivals.domain.ticket.repository.TicketStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketStockRollbacker {

    private final TicketRepository ticketRepository;
    private final TicketStockRepository ticketStockRepository;

    @Transactional
    public void rollbackTicketStock(Long ticketId, int quantity) {
        log.debug("티켓 재고 복구 - 티켓 ID: {}, 수량: {}", ticketId, quantity);
        TicketStock ticketStock = ticketStockRepository.findByTicketForUpdate(
                        ticketRepository.getReferenceById(ticketId))
                .orElseThrow(() -> new IllegalArgumentException("해당 티켓의 재고 정보가 존재하지 않습니다."));
        ticketStock.increaseStock(quantity);
    }
}