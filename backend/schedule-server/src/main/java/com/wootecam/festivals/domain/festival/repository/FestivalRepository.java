package com.wootecam.festivals.domain.festival.repository;

import com.wootecam.festivals.domain.festival.entity.Festival;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface FestivalRepository extends JpaRepository<Festival, Long> {

    @Modifying
    @Query("UPDATE Festival f SET f.festivalProgressStatus = 'COMPLETED' WHERE f.festivalProgressStatus != 'COMPLETED' AND f.endTime <= :now")
    void bulkUpdateCOMPLETEDFestivals(LocalDateTime now);

    @Modifying
    @Query("UPDATE Festival f SET f.festivalProgressStatus = 'ONGOING' WHERE f.festivalProgressStatus = 'UPCOMING' AND f.startTime <= :now")
    void bulkUpdateONGOINGFestivals(LocalDateTime now);

    @Query("SELECT f FROM Festival f WHERE f.festivalProgressStatus != 'COMPLETED' AND f.isDeleted = false")
    List<Festival> findFestivalsWithRestartScheduler();
}
