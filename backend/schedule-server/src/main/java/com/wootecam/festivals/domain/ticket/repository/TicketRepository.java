package com.wootecam.festivals.domain.ticket.repository;

import com.wootecam.festivals.domain.festival.dto.TicketResponse;
import com.wootecam.festivals.domain.ticket.entity.Ticket;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Query("""
                SELECT new com.wootecam.festivals.domain.festival.dto.TicketResponse(
                        t.id, t.name, t.detail, t.price, t.quantity, 
                        (SELECT count(ts.id) FROM TicketStock ts WHERE ts.ticket.id = t.id AND ts.memberId IS NULL),
                        t.startSaleTime, t.endSaleTime, t.refundEndTime, t.createdAt, t.updatedAt
                    ) 
                    FROM Ticket t 
                    WHERE t.id = :ticketId
                    AND t.isDeleted = false
            """)
    Optional<TicketResponse> findUpcomingAndOngoingSaleTickets(Long ticketId);
}
