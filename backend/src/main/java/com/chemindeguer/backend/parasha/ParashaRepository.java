package com.chemindeguer.backend.parasha;

import com.chemindeguer.backend.article.StatutArticle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParashaRepository extends JpaRepository<ParashaSummary, Long> {
    Optional<ParashaSummary> findBySlug(String slug);

    Optional<ParashaSummary> findBySlugAndStatut(String slug, StatutArticle statut);

    boolean existsBySlugAndStatut(String slug, StatutArticle statut);

    List<ParashaSummary> findAllByStatutOrderByPositionAsc(StatutArticle statut);

    List<ParashaSummary> findAllByOrderByPositionAsc();
}
