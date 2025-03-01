package com.wootecam.festivals.domain.festival.service;

import com.wootecam.festivals.domain.festival.dto.Cursor;
import com.wootecam.festivals.domain.festival.dto.FestivalCreateRequest;
import com.wootecam.festivals.domain.festival.dto.FestivalIdResponse;
import com.wootecam.festivals.domain.festival.dto.FestivalListResponse;
import com.wootecam.festivals.domain.festival.dto.FestivalResponse;
import com.wootecam.festivals.domain.festival.dto.KeySetPageResponse;
import com.wootecam.festivals.domain.festival.entity.Festival;
import com.wootecam.festivals.domain.festival.exception.FestivalErrorCode;
import com.wootecam.festivals.domain.festival.repository.FestivalRepository;
import com.wootecam.festivals.domain.member.entity.Member;
import com.wootecam.festivals.domain.member.repository.MemberRepository;
import com.wootecam.festivals.global.exception.GlobalErrorCode;
import com.wootecam.festivals.global.exception.type.ApiException;
import com.wootecam.festivals.global.utils.DateTimeUtils;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * 축제 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FestivalService {

    private final FestivalRepository festivalRepository;
    private final MemberRepository memberRepository;
    private final FestivalScheduleEventProducer festivalScheduleEventProducer;

    /**
     * 새로운 축제를 생성합니다.
     *
     * @param requestDto 축제 생성 요청 DTO
     * @return 생성된 축제의 ID를 포함한 응답 DTO
     */
    @Transactional
    @CacheEvict(value = "festivalsFirstPage")
    public FestivalIdResponse createFestival(FestivalCreateRequest requestDto, Long adminId) {
        Member admin = memberRepository.findById(adminId)
                .orElseThrow(() -> new ApiException(GlobalErrorCode.INVALID_REQUEST_PARAMETER, "유효하지 않는 멤버입니다."));

        Festival festival = requestDto.toEntity(admin);

        Festival savedFestival = festivalRepository.save(festival);

        festivalScheduleEventProducer.sendEvent(savedFestival);

        return new FestivalIdResponse(savedFestival.getId());
    }

    /**
     * 특정 ID의 축제 상세 정보를 조회합니다.
     *
     * @param festivalId 조회할 축제의 ID
     * @return 축제 상세 정보 DTO
     * @throws ApiException 축제를 찾을 수 없는 경우 발생
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "festival", key = "#festivalId", condition = "#festivalId != null")
    public FestivalResponse getFestivalDetail(Long festivalId) {
        Assert.notNull(festivalId, "Festival ID는 null일 수 없습니다.");

        log.debug("Festival 상세 정보 조회 - ID: {}", festivalId);

        Festival festival = festivalRepository.findById(festivalId)
                .orElseThrow(() -> {
                    log.warn("Festival을 찾을 수 없습니다 - ID: {}", festivalId);
                    return new ApiException(FestivalErrorCode.FESTIVAL_NOT_FOUND,
                            "Festival을 찾을 수 없습니다 - ID: " + festivalId); // + 연산의 경우 StringBuilder로 최적화된다.
                });

        log.debug("Festival 조회 완료: {}", festival);
        return FestivalResponse.from(festival);
    }

    /**
     * 커서 기반 페이지네이션을 사용하여 다가오는 축제 목록을 조회합니다.
     * Pageable을 사용하여 결과의 개수를 제한합니다.
     * 이는 setMaxResults()를 사용하는 것과 같은 효과를 내지만, 데이터베이스에 독립적이고 JPA의 추상화 수준을 유지합니다.
     * pageSize + 1을 요청하여 다음 페이지의 존재 여부를 확인합니다.
     *
     * @param cursorTime 커서 시간
     * @param cursorId   커서 ID
     * @param pageSize   페이지 크기
     * @return 축제 목록과 다음 페이지 커서 정보를 포함한 응답 DTO
     */
    @Transactional(readOnly = true)
    @Cacheable(
            value = "festivalsFirstPage",
            key = "#cursorTime + '_' + #cursorId + '_' + #pageSize",
            condition = "#cursorTime == null && #cursorId == null && #pageSize > 0"
    )
    public KeySetPageResponse<FestivalListResponse> getFestivals(LocalDateTime cursorTime,
                                                                 Long cursorId,
                                                                 int pageSize) {
        LocalDateTime now = DateTimeUtils.normalizeDateTime(LocalDateTime.now());
        Pageable pageRequest = PageRequest.of(0, pageSize + 1);

        List<FestivalListResponse> festivals = festivalRepository.findUpcomingFestivalsBeforeCursor(
                cursorTime != null ? cursorTime : now,
                cursorId != null ? cursorId : 0L,  // 변경: Long.MAX_VALUE 대신 0L 사용
                now,
                pageRequest);

        boolean hasNext = festivals.size() > pageSize;
        List<FestivalListResponse> pageContent = hasNext ? festivals.subList(0, pageSize) : festivals;

        Cursor nextCursor = null;
        if (hasNext) {
            FestivalListResponse lastFestival = pageContent.get(pageContent.size() - 1);
            nextCursor = new Cursor(lastFestival.startTime(), lastFestival.festivalId());
        }

        return new KeySetPageResponse<>(pageContent, nextCursor, hasNext);
    }
}
