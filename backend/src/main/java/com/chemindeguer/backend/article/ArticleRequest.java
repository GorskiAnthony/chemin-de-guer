package com.chemindeguer.backend.article;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ArticleRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 100) @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$|^$", message = "slug invalide") String slug,
        @Size(max = 300) String description,
        @NotBlank @Size(max = 100_000) String content,
        StatutArticle statut,
        List<Long> tagIds,
        Boolean commentsOpen) {
}
