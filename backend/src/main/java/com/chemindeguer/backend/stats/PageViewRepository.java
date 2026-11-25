package com.chemindeguer.backend.stats;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface PageViewRepository extends JpaRepository<PageView, Long> {
    @Query("SELECT v.day, count(v), count(distinct v.visitor) FROM PageView v WHERE v.day >= :from GROUP BY v.day ORDER BY v.day")
    List<Object[]> perDay(LocalDate from);

    @Query("SELECT v.path, count(v), count(distinct v.visitor) FROM PageView v WHERE v.day >= :from GROUP BY v.path ORDER BY count(v) DESC")
    List<Object[]> topPages(LocalDate from, Pageable limit);

    @Query("SELECT v.referrerHost, count(v) FROM PageView v WHERE v.day >= :from AND v.referrerHost IS NOT NULL GROUP BY v.referrerHost ORDER BY count(v) DESC")
    List<Object[]> topReferrers(LocalDate from, Pageable limit);

    @Query("SELECT v.device, count(v) FROM PageView v WHERE v.day >= :from GROUP BY v.device ORDER BY count(v) DESC")
    List<Object[]> devices(LocalDate from);

    @Modifying
    @Query("DELETE FROM PageView v WHERE v.day < :before")
    int deleteOlderThan(LocalDate before);
}
