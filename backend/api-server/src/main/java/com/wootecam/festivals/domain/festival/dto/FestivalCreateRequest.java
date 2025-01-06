package com.wootecam.festivals.domain.festival.dto;


import static com.wootecam.festivals.domain.festival.entity.FestivalValidConstant.*;

import com.wootecam.festivals.domain.festival.entity.Festival;
import com.wootecam.festivals.domain.member.entity.Member;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
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
                                    @Future(message = START_TIME_FUTURE_MESSAGE)
                                    LocalDateTime startTime,

                                    @NotNull(message = END_TIME_NULL_MESSAGE)
                                    @Future(message = END_TIME_FUTURE_MESSAGE)
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

    @AssertTrue(message = END_TIME_AFTER_START_TIME_MESSAGE)
    private boolean isEndTimeAfterStartTime() {
        return endTime != null && startTime != null && endTime.isAfter(startTime);
    }
}