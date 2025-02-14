package com.wootecam.festivals.domain.ticket.entity;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.wootecam.festivals.domain.festival.entity.Festival;
import com.wootecam.festivals.domain.festival.stub.FestivalStub;
import com.wootecam.festivals.global.utils.DateTimeUtils;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TicketTest {

    @Test
    @DisplayName("티켓 생성에 성공한다.")
    void createTicket() {
        LocalDateTime now = LocalDateTime.now();
        Festival festival = FestivalStub.createFestivalWithTime(now, LocalDateTime.now().plusDays(7));
        Ticket ticket = Ticket.builder()
                .festival(festival)
                .name("티켓 이름")
                .detail("티켓 상세")
                .price(10000L)
                .quantity(100)
                .startSaleTime(now)
                .endSaleTime(now.plusDays(1))
                .refundEndTime(now.plusDays(1))
                .build();

        assertAll(
                () -> assertEquals(festival, ticket.getFestival()),
                () -> assertEquals("티켓 이름", ticket.getName()),
                () -> assertEquals("티켓 상세", ticket.getDetail()),
                () -> assertEquals(10000L, ticket.getPrice()),
                () -> assertEquals(100, ticket.getQuantity()),
                () -> assertEquals(DateTimeUtils.normalizeDateTime(now), ticket.getStartSaleTime()),
                () -> assertEquals(DateTimeUtils.normalizeDateTime(now.plusDays(1)), ticket.getEndSaleTime()),
                () -> assertEquals(DateTimeUtils.normalizeDateTime(now.plusDays(1)), ticket.getRefundEndTime())
        );
    }
}
