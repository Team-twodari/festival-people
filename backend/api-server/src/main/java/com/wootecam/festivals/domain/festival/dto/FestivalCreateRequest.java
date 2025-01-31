package com.wootecam.festivals.domain.festival.dto;


import static com.wootecam.festivals.domain.festival.entity.FestivalValidConstant.DESCRIPTION_BLANK_MESSAGE;
import static com.wootecam.festivals.domain.festival.entity.FestivalValidConstant.DESCRIPTION_SIZE_MESSAGE;
import static com.wootecam.festivals.domain.festival.entity.FestivalValidConstant.END_TIME_AFTER_START_TIME_MESSAGE;
import static com.wootecam.festivals.domain.festival.entity.FestivalValidConstant.END_TIME_FUTURE_MESSAGE;
import static com.wootecam.festivals.domain.festival.entity.FestivalValidConstant.END_TIME_NULL_MESSAGE;
import static com.wootecam.festivals.domain.festival.entity.FestivalValidConstant.MAX_DESCRIPTION_LENGTH;
import static com.wootecam.festivals.domain.festival.entity.FestivalValidConstant.MAX_TITLE_LENGTH;
import static com.wootecam.festivals.domain.festival.entity.FestivalValidConstant.MIN_TITLE_LENGTH;
import static com.wootecam.festivals.domain.festival.entity.FestivalValidConstant.START_TIME_FUTURE_MESSAGE;
import static com.wootecam.festivals.domain.festival.entity.FestivalValidConstant.START_TIME_NULL_MESSAGE;
import static com.wootecam.festivals.domain.festival.entity.FestivalValidConstant.TITLE_BLANK_MESSAGE;
import static com.wootecam.festivals.domain.festival.entity.FestivalValidConstant.TITLE_SIZE_MESSAGE;

import com.wootecam.festivals.domain.festival.entity.Festival;
import com.wootecam.festivals.domain.member.entity.Member;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record FestivalCreateRequest(@NotBlank(message = TITLE_BLANK_MESSAGE)
                                    @Size(min = MIN_TITLE_LENGTH, max = MAX_TITLE_LENGTH, message = TITLE_SIZE_MESSAGE)
                                    String title,

                                    @NotBlank(message = DESCRIPTION_BLANK_MESSAGE)
                                    @Size(max = MAX_DESCRIPTION_LENGTH, message = DESCRIPTION_SIZE_MESSAGE)
                                    String description,

                                    @NotNull(message = START_TIME_NULL_MESSAGE)
                                    LocalDateTime startTime,

                                    @NotNull(message = END_TIME_NULL_MESSAGE)
                                    LocalDateTime endTime) {

    public Festival toEntity(Member admin) {
        return Festival.builder()
                .admin(admin)
                .title(title)
                .description(description)
                .startTime(startTime)
                .endTime(endTime)
                .build();
    }

    @AssertTrue(message = START_TIME_FUTURE_MESSAGE)
    private boolean isStartTimeInFuture() {
        // 현재 시간 기준으로 +1분을 고려하여 startTime이 미래인지 확인
        return startTime != null && startTime.isAfter(LocalDateTime.now().minusMinutes(1));
    }

    @AssertTrue(message = END_TIME_FUTURE_MESSAGE)
    private boolean isEndTimeInFuture() {
        // 현재 시간 기준으로 +1분을 고려하여 endTime이 미래인지 확인
        return endTime != null && endTime.isAfter(LocalDateTime.now().minusMinutes(1));
    }

    @AssertTrue(message = END_TIME_AFTER_START_TIME_MESSAGE)
    private boolean isEndTimeAfterStartTime() {
        // 종료 시간이 시작 시간보다 1분 이상 뒤인지 확인
        return endTime != null && startTime != null && endTime.isAfter(startTime.minusMinutes(1));
    }
}
