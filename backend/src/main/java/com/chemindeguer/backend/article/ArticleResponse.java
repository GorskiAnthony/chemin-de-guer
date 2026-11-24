package com.chemindeguer.backend.article;

import com.chemindeguer.backend.tag.Tag;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record ArticleResponse(Long id, String title, String content, String excerpt, String authorEmail,
                              StatutArticle statut, Instant createdAt, Instant updatedAt, Instant publishedAt,
                              List<TagRef> tags) {

    public record TagRef(Long id, String name, String slug) {
    }

    private static final int EXCERPT_LENGTH = 220;

    public static ArticleResponse from(Article a) {
        List<TagRef> tags = a.getTags().stream()
                .sorted(Comparator.comparing(Tag::getName))
                .map(t -> new TagRef(t.getId(), t.getName(), t.getSlug()))
                .toList();
        return new ArticleResponse(a.getId(), a.getTitle(), a.getContent(), excerpt(a.getContent()),
                a.getAuthor().getEmail(), a.getStatut(), a.getCreatedAt(), a.getUpdatedAt(), a.getPublishedAt(), tags);
    }

    /** Version liste publique : pas de contenu complet ni d'email d'auteur. */
    public ArticleResponse summary() {
        return new ArticleResponse(id, title, null, excerpt, null, statut, createdAt, updatedAt, publishedAt, tags);
    }

    /** Version publique complète : sans email d'auteur. */
    public ArticleResponse publicView() {
        return new ArticleResponse(id, title, content, excerpt, null, statut, createdAt, updatedAt, publishedAt, tags);
    }

    static String excerpt(String content) {
        String plain = content.replaceAll("[#*_`>\\[\\]]", "").replaceAll("\\s+", " ").trim();
        return plain.length() <= EXCERPT_LENGTH ? plain : plain.substring(0, EXCERPT_LENGTH).trim() + "…";
    }
}
