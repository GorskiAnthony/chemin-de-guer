package com.chemindeguer.backend.fete;

import com.chemindeguer.backend.article.StatutArticle;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class FeteDtos {
    private FeteDtos() {
    }

    public record SectionDto(@Size(max = 80) String label, @Size(max = 20_000) String content) {
    }

    public record FeteRequest(
            @NotBlank @Size(max = 80) String name,
            @Size(max = 80) String hebrewName,
            @Size(max = 50_000) String intro,
            @Valid @Size(max = 15) List<SectionDto> sections,
            StatutArticle statut) {
    }

    /** nextDate : prochaine occurrence calculée à partir du calendrier hébraïque (null si inconnue). */
    public record FeteResponse(Long id, String name, String slug, String hebrewName, String category, String intro,
                               List<SectionDto> sections, StatutArticle statut, Instant createdAt, Instant updatedAt, LocalDate nextDate) {
        static FeteResponse from(FeteSummary f, LocalDate nextDate) {
            return new FeteResponse(f.getId(), f.getName(), f.getSlug(), f.getHebrewName(), f.getCategory(), f.getIntro(),
                    f.getSections().stream().map(s -> new SectionDto(s.getLabel(), s.getContent())).toList(),
                    f.getStatut(), f.getCreatedAt(), f.getUpdatedAt(), nextDate);
        }

        /** Liste : sans le contenu des sections. */
        FeteResponse summary() {
            return new FeteResponse(id, name, slug, hebrewName, category, null, null, statut, createdAt, updatedAt, nextDate);
        }
    }
}
