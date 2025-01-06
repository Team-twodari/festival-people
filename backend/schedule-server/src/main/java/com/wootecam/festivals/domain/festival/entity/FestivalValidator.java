package com.wootecam.festivals.domain.festival.entity;

import com.wootecam.festivals.domain.member.entity.Member;
import java.time.LocalDateTime;
import java.util.Objects;

public class FestivalValidator {

    private FestivalValidator() {
    }

    public static void validateFestival(Member admin, String title, String description,
                                        LocalDateTime startTime, LocalDateTime endTime) {
        Objects.requireNonNull(admin, FestivalValidConstant.ADMIN_NULL_MESSAGE);
        validateTitle(title);
        validateDescription(description);
        validateTimeRange(startTime, endTime);
    }

    public static void validateTitle(String title) {
        Objects.requireNonNull(title, FestivalValidConstant.TITLE_NULL_MESSAGE);
        if (title.isEmpty()) {
            throw new IllegalArgumentException(FestivalValidConstant.TITLE_EMPTY_MESSAGE);
        }
        if (title.length() > FestivalValidConstant.MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException(FestivalValidConstant.TITLE_LENGTH_MESSAGE);
        }
    }

    public static void validateDescription(String description) {
        Objects.requireNonNull(description, FestivalValidConstant.DESCRIPTION_NULL_MESSAGE);
        if (description.isEmpty()) {
            throw new IllegalArgumentException(FestivalValidConstant.DESCRIPTION_EMPTY_MESSAGE);
        }
        if (description.length() > FestivalValidConstant.MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(FestivalValidConstant.DESCRIPTION_LENGTH_MESSAGE);
        }
    }

    public static void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        Objects.requireNonNull(startTime, FestivalValidConstant.START_TIME_NULL_MESSAGE);
        Objects.requireNonNull(endTime, FestivalValidConstant.END_TIME_NULL_MESSAGE);

        LocalDateTime now = LocalDateTime.now().minusMinutes(1); // 1분의 여유를 둡니다.

        if (startTime.isBefore(now)) {
            throw new IllegalArgumentException(FestivalValidConstant.START_TIME_FUTURE_MESSAGE);
        }

        if (endTime.isBefore(now)) {
            throw new IllegalArgumentException(FestivalValidConstant.END_TIME_FUTURE_MESSAGE);
        }

        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException(FestivalValidConstant.TIME_RANGE_MESSAGE);
        }
    }

    public static boolean isValidStatusTransition(FestivalProgressStatus currentStatus,
                                                  FestivalProgressStatus newStatus) {
        return switch (currentStatus) {
            case UPCOMING ->
                    newStatus == FestivalProgressStatus.ONGOING || newStatus == FestivalProgressStatus.COMPLETED;
            case ONGOING -> newStatus == FestivalProgressStatus.COMPLETED;
            case COMPLETED -> false; // 종료 상태에서는 다른 상태로 변경할 수 없음
            default -> false;
        };
    }
}