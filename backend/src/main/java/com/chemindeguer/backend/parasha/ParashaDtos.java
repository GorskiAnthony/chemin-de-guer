package com.chemindeguer.backend.parasha;

import com.chemindeguer.backend.article.StatutArticle;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class ParashaDtos {
    private ParashaDtos() {
    }

    public record AliyahDto(@Size(max = 80) String label, @Size(max = 20_000) String content) {
    }

    public record ParashaRequest(
            @NotBlank @Size(max = 80) String name,
            @Size(max = 80) String hebrewName,
            @Size(max = 50_000) String intro,
            @Valid @Size(max = 12) List<AliyahDto> aliyot,
            StatutArticle statut) {
    }

    public record ParashaResponse(Long id, String name, String slug, String hebrewName, String book, String intro,
                                  List<AliyahDto> aliyot, StatutArticle statut, Instant createdAt, Instant updatedAt) {
        static ParashaResponse from(ParashaSummary p) {
            return new ParashaResponse(p.getId(), p.getName(), p.getSlug(), p.getHebrewName(), p.getBook(), p.getIntro(),
                    p.getAliyot().stream().map(a -> new AliyahDto(a.getLabel(), a.getContent())).toList(),
                    p.getStatut(), p.getCreatedAt(), p.getUpdatedAt());
        }

        /** Liste publique : sans le contenu des montées. */
        ParashaResponse summary() {
            return new ParashaResponse(id, name, slug, hebrewName, book, null, null, statut, createdAt, updatedAt);
        }
    }
}
