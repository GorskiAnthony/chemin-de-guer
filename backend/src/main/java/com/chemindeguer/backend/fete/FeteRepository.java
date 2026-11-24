package com.chemindeguer.backend.fete;

import com.chemindeguer.backend.article.StatutArticle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeteRepository extends JpaRepository<FeteSummary, Long> {
    Optional<FeteSummary> findBySlug(String slug);

    Optional<FeteSummary> findBySlugAndStatut(String slug, StatutArticle statut);

    boolean existsBySlugAndStatut(String slug, StatutArticle statut);

    List<FeteSummary> findAllByStatutOrderByPositionAscNameAsc(StatutArticle statut);

    List<FeteSummary> findAllByOrderByPositionAscNameAsc();
}
