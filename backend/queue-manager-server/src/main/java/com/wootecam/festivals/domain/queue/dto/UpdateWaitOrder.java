package com.wootecam.festivals.domain.queue.dto;

public record UpdateWaitOrder(Long ticketId,
                              Integer waitOrder,
                              long updatedAt) {
}
