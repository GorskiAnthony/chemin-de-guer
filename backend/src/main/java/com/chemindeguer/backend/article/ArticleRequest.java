package com.chemindeguer.backend.article;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ArticleRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 100_000) String content,
        StatutArticle statut,
        List<Long> tagIds) {
}
