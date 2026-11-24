package com.chemindeguer.backend.comment;

import com.chemindeguer.backend.article.ArticleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;

    public CommentService(CommentRepository commentRepository, ArticleRepository articleRepository) {
        this.commentRepository = commentRepository;
        this.articleRepository = articleRepository;
    }

    @Transactional(readOnly = true)
    public List<CommentController.CommentResponse> approvedFor(Long articleId) {
        return commentRepository.findApprovedByArticle(articleId).stream()
                .map(c -> CommentController.CommentResponse.from(c, false)).toList();
    }

    @Transactional
    public void submit(Long articleId, String authorName, String content) {
        var article = articleRepository.findPublishedById(articleId)
                .orElseThrow(() -> new NoSuchElementException("Article non trouvé"));
        var comment = new Comment();
        comment.setArticle(article);
        comment.setAuthorName(authorName.trim());
        comment.setContent(content.trim());
        comment.setCreatedAt(Instant.now());
        comment.setStatut(StatutCommentaire.PENDING);
        commentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentController.CommentResponse> findAll() {
        return commentRepository.findAllWithArticle().stream()
                .map(c -> CommentController.CommentResponse.from(c, true)).toList();
    }

    @Transactional
    public void approve(Long id) {
        var comment = commentRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Commentaire non trouvé"));
        comment.setStatut(StatutCommentaire.APPROVED);
    }

    @Transactional
    public void delete(Long id) {
        if (!commentRepository.existsById(id)) {
            throw new NoSuchElementException("Commentaire non trouvé");
        }
        commentRepository.deleteById(id);
    }
}
