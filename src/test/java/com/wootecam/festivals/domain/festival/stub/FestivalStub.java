package com.wootecam.festivals.domain.festival.stub;

import com.wootecam.festivals.domain.festival.entity.Festival;
import java.time.LocalDateTime;

public class FestivalStub extends Festival {

    private final Long id;
    private final String title;
    private final String description;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;

    private FestivalStub(Long id, String title, String description,
                         LocalDateTime startTime, LocalDateTime endTime) {
        super();
        this.id = id;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static FestivalStub createValidFestival(Long id) {
        return new FestivalStub(id,
                "Test Festival", "Test Description",
                LocalDateTime.now(), LocalDateTime.now().plusDays(7));
    }

    public static FestivalStub createFestivalWithNullId() {
        return new FestivalStub(null,
                "Test Festival", "Test Description",
                LocalDateTime.now(), LocalDateTime.now().plusDays(7));
    }

    public static FestivalStub createFestivalWithTime(LocalDateTime festivalStartTime, LocalDateTime festivalEndTime) {
        return new FestivalStub(null,
                "Test Festival", "Test Description",
                festivalStartTime, festivalEndTime);
    }

    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public LocalDateTime getStartTime() {
        return startTime;
    }

    @Override
    public LocalDateTime getEndTime() {
        return endTime;
    }
}