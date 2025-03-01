package com.wootecam.festivals.domain.purchase.service;

import com.wootecam.festivals.domain.member.entity.Member;
import com.wootecam.festivals.domain.member.repository.MemberRepository;
import com.wootecam.festivals.domain.payment.entity.Payment;
import com.wootecam.festivals.domain.purchase.dto.PurchasableResponse;
import com.wootecam.festivals.domain.purchase.dto.PurchasePreviewInfoResponse;
import com.wootecam.festivals.domain.purchase.exception.PurchaseErrorCode;
import com.wootecam.festivals.domain.purchase.repository.PurchaseRepository;
import com.wootecam.festivals.domain.ticket.entity.Ticket;
import com.wootecam.festivals.domain.ticket.entity.TicketStock;
import com.wootecam.festivals.domain.ticket.exception.TicketErrorCode;
import com.wootecam.festivals.domain.ticket.repository.PurchaseSessionRedisRepository;
import com.wootecam.festivals.domain.ticket.repository.TicketRepository;
import com.wootecam.festivals.domain.ticket.repository.TicketStockRepository;
import com.wootecam.festivals.global.auth.purchase.PurchaseSession;
import com.wootecam.festivals.global.exception.type.ApiException;
import com.wootecam.festivals.global.utils.TimeProvider;
import com.wootecam.festivals.global.utils.UuidProvider;
import jakarta.persistence.PersistenceException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 티켓 결제 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseService {

    @Value("${purchase.session.ttl:5}")
    private Long purchaseSessionTtl;

    private final PurchaseRepository purchaseRepository;
    private final TicketStockRepository ticketStockRepository;
    private final MemberRepository memberRepository;
    private final TicketRepository ticketRepository;
    private final TimeProvider timeProvider;
    private final UuidProvider uuidProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final PurchaseSessionRedisRepository purchaseSessionRedisRepository;

    /**
     * 티켓 구매 권한이 유효한지 확인합니다.
     *
     * @param purchaseSessionId
     * @param ticketId
     * @param loginMemberId
     */
    public PurchaseSession validPurchasableMember(String purchaseSessionId, Long ticketId, Long loginMemberId) {
        String value = purchaseSessionRedisRepository.getPurchaseSessionValue(ticketId, purchaseSessionId,
                loginMemberId);
        if (value == null) {
            log.warn("유효한 구매 세션이 아닙니다. - 구매 세션 ID: {}", purchaseSessionId);
            throw new ApiException(PurchaseErrorCode.PURCHASE_SESSION_EXPIRED);
        }

        return PurchaseSession.of(value);
    }

    /**
     * 티켓을 결제할 수 있는지 확인합니다. 티켓을 재고가 없다면 false인 PurchasableResponse을, 티켓 재고가 있다면 true인 PurchasableResponse을 반환합니다. 티켓 구매
     * 시각이 아니거나, 이미 티켓을 구매했다면 예외를 발생시킵니다. 티켓을 구매할 수 있다면 티켓 재고를 차감하고, 구매 가능한 세션을 발급하고 저장합니다.
     *
     * @param ticketId
     * @param loginMemberId
     * @param now
     * @return 구매 가능 여부 응답
     */
    @Transactional
    public PurchasableResponse checkPurchasable(Long ticketId, Long loginMemberId, LocalDateTime now) {
        Ticket ticket = findTicketById(ticketId);
        validTicketPurchasableTime(now, ticket);

        Member member = memberRepository.getReferenceById(loginMemberId);
        validFirstTicketPurchase(ticket, member);
        validFirstTicketStockReservation(ticket, member);

        Optional<TicketStock> optionalTicketStock = getTicketStockForUpdate(ticket);
        if (optionalTicketStock.isEmpty() || optionalTicketStock.get().isReserved()) {
            return new PurchasableResponse(false, null);
        }

        TicketStock ticketStock = optionalTicketStock.get();
        reserveTicket(ticketStock, member.getId());

        String sessionId = uuidProvider.getUuid();
        purchaseSessionRedisRepository.addPurchaseSession(ticketId, loginMemberId, sessionId, ticketStock.getId(),
                purchaseSessionTtl);

        return new PurchasableResponse(true, sessionId);
    }

    /**
     * 티켓 결제창에서 보여줄 정보를 조회합니다. (결제 창은 구매 버튼을 누르고 난 후 보이는 화면)
     *
     * @param memberId
     * @param festivalId
     * @param ticketId
     * @param ticketStockId
     * @return PurchasePreviewInfoResponse
     */
    public PurchasePreviewInfoResponse getPurchasePreviewInfo(Long memberId, Long festivalId, Long ticketId,
                                                              Long ticketStockId) {
        Ticket ticket = findTicketByIdAndFestivalId(ticketId, festivalId);
        validTicketPurchasableTime(LocalDateTime.now(), ticket);

        Member member = memberRepository.getReferenceById(memberId);
        validFirstTicketPurchase(ticket, member);

        TicketStock ticketStock = getTicketStock(festivalId, ticket, ticketStockId, memberId);

        return new PurchasePreviewInfoResponse(festivalId, ticket.getFestival().getTitle(),
                ticket.getFestival().getFestivalImg(),
                ticket.getId(), ticket.getName(), ticket.getDetail(), ticket.getPrice(), ticket.getQuantity(),
                ticketStock.getId(),
                ticket.getEndSaleTime());
    }

    private Optional<TicketStock> getTicketStockForUpdate(Ticket ticket) {
        return ticketStockRepository.findByTicketForUpdate(ticket.getId());
    }

    private TicketStock getTicketStock(Long festivalId, Ticket ticket, Long ticketStockId, Long memberId) {
        TicketStock ticketStock = ticketStockRepository.findByIdAndTicketIdAndMemberId(ticketStockId, ticket.getId(),
                        memberId)
                .orElseThrow(() -> {
                    log.warn("티켓 재고를 찾을 수 없습니다. 티켓 ID: {}, 페스티벌 ID: {}, 티켓 재고 ID: {}", ticket.getId(), festivalId,
                            ticketStockId);
                    return new ApiException(TicketErrorCode.TICKET_STOCK_NOT_FOUND);
                });

        if (!ticketStock.getTicket().getId().equals(ticket.getId())) {
            log.warn("티켓 재고가 일치하지 않음 - 티켓 ID: {}, 페스티벌 ID: {}, 티켓 재고 ID: {}", ticket.getId(), festivalId,
                    ticketStockId);
            throw new ApiException(TicketErrorCode.TICKET_STOCK_MISMATCH);
        }
        return ticketStock;
    }

    private void reserveTicket(TicketStock ticketStock, Long buyerId) {
        try {
            ticketStock.reserveTicket(buyerId);
            ticketStockRepository.flush();
        } catch (PersistenceException e) {
            if (e.getCause() instanceof SQLIntegrityConstraintViolationException) {
                log.warn("이미 티켓 재고를 예약한 회원입니다. - 티켓 ID: {}", ticketStock.getTicket().getId());
                throw new ApiException(TicketErrorCode.ALREADY_RESERVED_TICKET_STOCK);
            }
            throw e;
        } catch (IllegalStateException e) {
            log.warn("티켓 재고 부족 - 티켓 ID: {}", ticketStock.getTicket().getId());
            throw new ApiException(TicketErrorCode.TICKET_STOCK_EMPTY);
        }
    }

    private void validTicketPurchasableTime(LocalDateTime now, Ticket ticket) {
        if (now.isBefore(ticket.getStartSaleTime()) || now.isAfter(ticket.getEndSaleTime())) {
            log.warn("유효하지 않은 티켓 구매 시간 - 티켓 ID: {}, 현재 시간: {}", ticket.getId(), now);
            throw new ApiException(PurchaseErrorCode.INVALID_TICKET_PURCHASE_TIME);
        }
    }

    private void validFirstTicketPurchase(Ticket ticket, Member member) {
        if (purchaseRepository.existsByTicketAndMember(ticket, member)) {
            log.warn("이미 구매한 티켓 - 티켓 ID: {}, 회원 ID: {}", ticket.getId(), member.getId());
            throw new ApiException(PurchaseErrorCode.ALREADY_PURCHASED_TICKET);
        }
    }

    // 해당 유저가 점유한 티켓 재고가 있는지 확인하는 로직 -> 없으면 에러(있어야 구매 가능)
    private void validReservationTicketStock(Long ticketStockId, Long ticketId, Long memberId) {
        if (!ticketStockRepository.existsByIdAndTicketIdAndMemberId(ticketStockId, ticketId, memberId)) {
            log.warn("해당 유저의 티켓 재고를 찾을 수 없습니다. 티켓 ID: {}, 티켓 재고 ID: {}, 구매자 ID: {}", ticketId, ticketStockId,
                    memberId);
            throw new ApiException(TicketErrorCode.TICKET_STOCK_MISMATCH);
        }
    }

    // 페스티벌 ID와 티켓 ID로 티켓을 찾는 메소드, join fetch를 사용하여 티켓과 페스티벌을 한번에 가져옴
    private Ticket findTicketByIdAndFestivalId(Long ticketId, Long festivalId) {
        return ticketRepository.findByIdAndFestivalId(ticketId, festivalId)
                .orElseThrow(() -> {
                    log.warn("티켓 또는 페스티벌을 찾을 수 없습니다. 티켓 ID: {}, 축제 ID: {}", ticketId, festivalId);
                    return new ApiException(TicketErrorCode.TICKET_NOT_FOUND);
                });
    }

    private Ticket findTicketById(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> {
                    log.warn("티켓을 찾을 수 없습니다. 티켓 ID: {}", ticketId);
                    return new ApiException(TicketErrorCode.TICKET_NOT_FOUND);
                });
    }

    // 해당 유저가 이미 티켓 재고를 예약했는지 확인하는 로직 -> 있으면 에러(중복 점유 막음)
    private void validFirstTicketStockReservation(Ticket ticket, Member member) {
        if (ticketStockRepository.existsByTicketAndMember(ticket, member.getId())) {
            log.warn("이미 티켓 재고를 예약한 회원입니다. 티켓 ID: {}, 회원 ID: {}", ticket.getId(), member.getId());
            throw new ApiException(TicketErrorCode.ALREADY_RESERVED_TICKET_STOCK);
        }
    }
}
