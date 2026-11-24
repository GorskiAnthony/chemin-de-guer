package com.chemindeguer.backend.comment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
public class CommentController {

    public record CommentRequest(@NotBlank @Size(max = 80) String authorName,
                                 @NotBlank @Size(max = 2000) String content,
                                 String website) {
    }

    public record CommentResponse(Long id, String authorName, String content, Instant createdAt,
                                  StatutCommentaire statut, Long articleId, String articleTitle) {
        static CommentResponse from(Comment c, boolean admin) {
            return new CommentResponse(c.getId(), c.getAuthorName(), c.getContent(), c.getCreatedAt(),
                    admin ? c.getStatut() : null,
                    admin ? c.getArticle().getId() : null,
                    admin ? c.getArticle().getTitle() : null);
        }
    }

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/api/public/articles/{articleId}/comments")
    public List<CommentResponse> approved(@PathVariable Long articleId) {
        return commentService.approvedFor(articleId);
    }

    /** Le champ "website" est un honeypot : les robots le remplissent, les humains non. */
    @PostMapping("/api/public/articles/{articleId}/comments")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void submit(@PathVariable Long articleId, @Valid @RequestBody CommentRequest request) {
        if (request.website() != null && !request.website().isBlank()) {
            return;
        }
        commentService.submit(articleId, request.authorName(), request.content());
    }

    @GetMapping("/api/comments")
    public List<CommentResponse> all() {
        return commentService.findAll();
    }

    @PostMapping("/api/comments/{id}/approve")
    public void approve(@PathVariable Long id) {
        commentService.approve(id);
    }

    @DeleteMapping("/api/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        commentService.delete(id);
    }
}
