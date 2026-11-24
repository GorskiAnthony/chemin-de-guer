package com.chemindeguer.backend.seo;

/** Réponse HTTP d'une page pré-rendue : statut, HTML et éventuelle redirection permanente. */
public record SeoPage(int status, String html, String redirect) {
    static SeoPage ok(String html) { return new SeoPage(200, html, null); }
    static SeoPage notFound(String html) { return new SeoPage(404, html, null); }
    static SeoPage redirect(String location) { return new SeoPage(301, null, location); }
}
