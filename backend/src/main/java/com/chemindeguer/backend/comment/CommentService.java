package com.chemindeguer.backend.comment;

import com.chemindeguer.backend.article.ArticleRepository;
import com.chemindeguer.backend.settings.SettingsService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class CommentService {
    private static final long MAX_PENDING = 300;

    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;
    private final SettingsService settings;
    private final CommentNotifier notifier;

    public CommentService(CommentRepository commentRepository, ArticleRepository articleRepository,
                          SettingsService settings, CommentNotifier notifier) {
        this.commentRepository = commentRepository;
        this.articleRepository = articleRepository;
        this.settings = settings;
        this.notifier = notifier;
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
        if (!settings.commentsEnabled() || !article.commentsAllowed()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Les commentaires sont fermés");
        }
        // Garde-fou : une file de modération déjà saturée n'accepte plus de nouveaux messages
        if (commentRepository.countByStatut(StatutCommentaire.PENDING) >= MAX_PENDING) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Trop de commentaires en attente, réessayez plus tard");
        }
        var comment = new Comment();
        comment.setArticle(article);
        comment.setAuthorName(authorName.trim());
        comment.setContent(content.trim());
        comment.setCreatedAt(Instant.now());
        comment.setStatut(StatutCommentaire.PENDING);
        commentRepository.save(comment);
        notifier.newComment(article.getTitle(), comment.getAuthorName(), comment.getContent());
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
