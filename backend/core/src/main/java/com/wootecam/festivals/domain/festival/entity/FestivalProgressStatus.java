package com.wootecam.festivals.domain.festival.entity;

import com.wootecam.festivals.global.docs.EnumType;

public enum FestivalProgressStatus implements EnumType {

    UPCOMING("예정"),
    ONGOING("진행중"),
    COMPLETED("종료"),
    ;

    private final String description;

    FestivalProgressStatus(String description) {
        this.description = description;
    }

    @Override
    public String getName() {
        return this.name();
    }

    @Override
    public String getDescription() {
        return description;
    }

    public static FestivalProgressStatus from(String name) {
        for (FestivalProgressStatus status : FestivalProgressStatus.values()) {
            if (status.name().equals(name)) {
                return status;
            }
        }
        throw new IllegalArgumentException("No matching constant for [" + name + "]");
    }
}
