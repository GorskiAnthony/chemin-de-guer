package com.chemindeguer.backend.article;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    Optional<Article> findByIdAndStatut(Long id, StatutArticle statut);

    @Query("SELECT a FROM Article a JOIN FETCH a.author WHERE a.id = :id")
    Optional<Article> findByIdWithAuthor(@Param("id") Long id);

    @Query("SELECT a FROM Article a JOIN FETCH a.author ORDER BY a.updatedAt DESC")
    List<Article> findAllWithAuthor();

    @Query("SELECT a FROM Article a JOIN FETCH a.author WHERE a.id = :id AND a.statut = 'PUBLISHED'")
    Optional<Article> findPublishedById(@Param("id") Long id);

    @Query("""
            SELECT a FROM Article a JOIN FETCH a.author
            WHERE a.statut = 'PUBLISHED'
              AND (LOWER(a.title) LIKE :q ESCAPE '\\' OR LOWER(a.content) LIKE :q ESCAPE '\\')
            ORDER BY a.publishedAt DESC
            """)
    List<Article> searchPublished(@Param("q") String q);

    @Query("""
            SELECT a FROM Article a JOIN FETCH a.author
            WHERE a.statut = 'PUBLISHED'
              AND EXISTS (SELECT 1 FROM a.tags t WHERE t.slug = :tag)
              AND (LOWER(a.title) LIKE :q ESCAPE '\\' OR LOWER(a.content) LIKE :q ESCAPE '\\')
            ORDER BY a.publishedAt DESC
            """)
    List<Article> searchPublishedByTag(@Param("tag") String tag, @Param("q") String q);

    @Query("SELECT a FROM Article a WHERE a.statut = 'PUBLISHED' ORDER BY a.publishedAt DESC")
    List<Article> findAllPublished();
}
