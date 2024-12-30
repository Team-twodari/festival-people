package com.wootecam.festivals.domain.ticket.service;

import com.wootecam.festivals.domain.festival.entity.Festival;
import com.wootecam.festivals.domain.festival.exception.FestivalErrorCode;
import com.wootecam.festivals.domain.festival.repository.FestivalRepository;
import com.wootecam.festivals.domain.ticket.dto.TicketCreateRequest;
import com.wootecam.festivals.domain.ticket.dto.TicketIdResponse;
import com.wootecam.festivals.domain.ticket.dto.TicketListResponse;
import com.wootecam.festivals.domain.festival.dto.TicketResponse;
import com.wootecam.festivals.domain.ticket.entity.Ticket;
import com.wootecam.festivals.domain.ticket.entity.TicketStock;
import com.wootecam.festivals.domain.ticket.repository.CurrentTicketWaitRedisRepository;
import com.wootecam.festivals.domain.ticket.repository.TicketInfoRedisRepository;
import com.wootecam.festivals.domain.ticket.repository.TicketRepository;
import com.wootecam.festivals.domain.ticket.repository.TicketStockCountRedisRepository;
import com.wootecam.festivals.domain.ticket.repository.TicketStockJdbcRepository;
import com.wootecam.festivals.global.exception.type.ApiException;
import com.wootecam.festivals.global.utils.TimeProvider;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 티켓 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketStockJdbcRepository ticketStockJdbcRepository;
    private final FestivalRepository festivalRepository;
    private final TicketCacheService ticketCacheService;

    private final TicketInfoRedisRepository ticketInfoRedisRepository;
    private final TicketStockCountRedisRepository ticketStockCountRedisRepository;
    private final CurrentTicketWaitRedisRepository currentTicketWaitRedisRepository;

    private final TimeProvider timeProvider;

    /**
     * 티켓 생성
     *
     * @param festivalId 축제 ID
     * @param request    티켓 생성 요청 DTO
     * @return 생성된 티켓의 ID
     */
    @Transactional
    @CacheEvict(value = "ticketList", key = "#festivalId")
    public TicketIdResponse createTicket(Long festivalId, TicketCreateRequest request) {
        log.debug("티켓 생성 요청 - 축제 ID: {}", festivalId);

        Festival festival = festivalRepository.findById(festivalId)
                .orElseThrow(() -> {
                    log.warn("축제를 찾을 수 없음 - 축제 ID: {}", festivalId);
                    return new ApiException(FestivalErrorCode.FESTIVAL_NOT_FOUND);
                });

        Ticket newTicket = ticketRepository.save(request.toEntity(festival));
        log.debug("티켓 엔티티 생성 - 티켓 ID: {}", newTicket.getId());
        ticketCacheService.cacheTicket(newTicket);

        List<TicketStock> ticketStock = newTicket.createTicketStock();
        ticketStockJdbcRepository.saveTicketStocks(ticketStock);
        log.debug("티켓 재고 생성 완료 - 티켓 ID: {}", newTicket.getId());

        TicketIdResponse response = new TicketIdResponse(newTicket.getId());
        log.debug("티켓 생성 완료 - 티켓 ID: {}", response.ticketId());

        LocalDateTime now = timeProvider.getCurrentTime();
        if (newTicket.isSaleOnTime(now) || now.plusMinutes(10).isAfter(newTicket.getStartSaleTime())
                || now.minusMinutes(1).isAfter(newTicket.getStartSaleTime())) {
            ticketInfoRedisRepository.setTicketInfo(newTicket.getId(), newTicket.getStartSaleTime(),
                    newTicket.getEndSaleTime());
            currentTicketWaitRedisRepository.addCurrentTicketWait(newTicket.getId());
            ticketStockCountRedisRepository.setTicketStockCount(newTicket.getId(), (long) newTicket.getQuantity());

            log.debug("티켓 정보 redis 업데이트 완료 - 티켓 ID: {}, 판매 시작 시각: {}, 판매 종료 시각: {}", newTicket.getId(),
                    newTicket.getStartSaleTime(), newTicket.getEndSaleTime());
        }
        return response;
    }

    /**
     * 축제 ID에 해당하는 티켓 목록 조회
     *
     * @param festivalId 축제 ID
     * @return 티켓 목록 응답 DTO
     */
    @Cacheable(value = "ticketList", key = "#festivalId")
    public TicketListResponse getTickets(Long festivalId) {
        log.debug("티켓 목록 조회 요청 - 축제 ID: {}", festivalId);

        if (!festivalRepository.existsById(festivalId)) {
            log.warn("축제를 찾을 수 없음 - 축제 ID: {}", festivalId);
            throw new ApiException(FestivalErrorCode.FESTIVAL_NOT_FOUND);
        }

        List<TicketResponse> ticketsByFestivalIdWithRemainStock =
                ticketRepository.findTicketsByFestivalIdWithRemainStock(festivalId);
        log.debug("티켓 목록: {}", ticketsByFestivalIdWithRemainStock);

        return new TicketListResponse(festivalId, ticketsByFestivalIdWithRemainStock);
    }
}
