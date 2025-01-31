package com.wootecam.festivals.domain.festival.repository;

import com.wootecam.festivals.domain.festival.entity.Festival;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface FestivalRepository extends JpaRepository<Festival, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Festival f where f.id = :id")
    Optional<Festival> findByIdForUpdate(@Param("id") Long id);
}
