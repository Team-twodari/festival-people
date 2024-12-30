package com.wootecam.festivals.domain.ticket.dto;

import com.wootecam.festivals.domain.festival.dto.TicketResponse;
import java.util.List;

public record TicketListResponse(Long festivalId, List<TicketResponse> tickets) {
}
