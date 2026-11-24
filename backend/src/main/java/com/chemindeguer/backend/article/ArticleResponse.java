package com.chemindeguer.backend.article;

import com.chemindeguer.backend.common.Excerpts;
import com.chemindeguer.backend.tag.Tag;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record ArticleResponse(Long id, String slug, String title, String content, String excerpt, String description,
                              String authorEmail, StatutArticle statut, Instant createdAt, Instant updatedAt,
                              Instant publishedAt, List<TagRef> tags, boolean commentsOpen) {

    public record TagRef(Long id, String name, String slug) {
    }

    private static final int EXCERPT_LENGTH = 220;

    public static ArticleResponse from(Article a) {
        List<TagRef> tags = a.getTags().stream()
                .sorted(Comparator.comparing(Tag::getName))
                .map(t -> new TagRef(t.getId(), t.getName(), t.getSlug()))
                .toList();
        return new ArticleResponse(a.getId(), a.getSlug(), a.getTitle(), a.getContent(), summaryText(a), a.getDescription(),
                a.getAuthor().getEmail(), a.getStatut(), a.getCreatedAt(), a.getUpdatedAt(), a.getPublishedAt(), tags,
                a.commentsAllowed());
    }

    /** Version liste publique : pas de contenu complet ni d'email d'auteur. */
    public ArticleResponse summary() {
        return new ArticleResponse(id, slug, title, null, excerpt, description, null, statut, createdAt, updatedAt, publishedAt, tags, commentsOpen);
    }

    /** Version publique complète : sans email d'auteur. */
    public ArticleResponse publicView() {
        return new ArticleResponse(id, slug, title, content, excerpt, description, null, statut, createdAt, updatedAt, publishedAt, tags, commentsOpen);
    }

    /** Côté public : commentaires ouverts seulement si l'interrupteur général ET celui de l'article le sont. */
    public ArticleResponse withCommentsOpen(boolean open) {
        return new ArticleResponse(id, slug, title, content, excerpt, description, authorEmail, statut, createdAt, updatedAt, publishedAt, tags, open);
    }

    /** La description rédigée par l'auteur, sinon le premier vrai paragraphe du texte. */
    static String summaryText(Article a) {
        if (a.getDescription() != null && !a.getDescription().isBlank()) return a.getDescription().trim();
        return Excerpts.of(a.getContent(), EXCERPT_LENGTH);
    }

    static String excerpt(String content) {
        return Excerpts.of(content, EXCERPT_LENGTH);
    }
}
