package com.chemindeguer.backend.common;

import java.text.Normalizer;
import java.util.Locale;

public final class Slugs {
    private Slugs() {
    }

    /** Slug d'article : jamais purement numérique (réservé aux anciens liens par identifiant), 80 caractères maximum. */
    public static String forArticle(String input) {
        String s = of(input == null ? "" : input);
        if (s.equals("tag")) s = "article";
        if (s.length() > 80) s = s.substring(0, 80).replaceAll("-+$", "");
        if (s.matches("\\d+")) s = "article-" + s;
        return s;
    }

    public static String of(String input) {
        String s = Normalizer.normalize(input.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return s.isEmpty() ? "tag" : s;
    }
}
