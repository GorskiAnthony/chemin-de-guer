package com.chemindeguer.backend.comment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query("SELECT c FROM Comment c WHERE c.article.id = :articleId AND c.statut = 'APPROVED' ORDER BY c.createdAt ASC")
    List<Comment> findApprovedByArticle(Long articleId);

    long countByStatut(StatutCommentaire statut);

    @Query("SELECT c FROM Comment c JOIN FETCH c.article ORDER BY c.createdAt DESC")
    List<Comment> findAllWithArticle();
}
