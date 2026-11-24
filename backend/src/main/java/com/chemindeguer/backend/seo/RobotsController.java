package com.chemindeguer.backend.seo;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

/** robots.txt dynamique : son contenu dépend du réglage SITE_INDEXABLE. Exposé par nginx sous /robots.txt. */
@RestController
public class RobotsController {
    /** Robots d'entraînement et de collecte pour les IA. */
    private static final List<String> AI_CRAWLERS = List.of("GPTBot", "ChatGPT-User", "OAI-SearchBot", "ClaudeBot", "Claude-User",
            "Claude-SearchBot", "anthropic-ai", "CCBot", "PerplexityBot", "Perplexity-User", "Google-Extended", "Applebot-Extended",
            "Bytespider", "Meta-ExternalAgent", "Amazonbot", "cohere-ai");

    private final SiteProperties site;

    public RobotsController(SiteProperties site) {
        this.site = site;
    }

    @GetMapping(value = "/api/robots.txt", produces = "text/plain;charset=UTF-8")
    public ResponseEntity<String> robots() {
        StringBuilder t = new StringBuilder();
        if (site.indexable()) {
            t.append("# Chemin de Guer : contenu public ouvert aux moteurs de recherche et aux moteurs de réponse (IA).\n")
                    .append("User-agent: *\nDisallow: /api/\n\n");
            for (String bot : AI_CRAWLERS) t.append("User-agent: ").append(bot).append("\n");
            t.append("Allow: /\nDisallow: /api/\n\n")
                    .append("# Version lisible par les IA : /llms.txt (résumé) et /llms-full.txt (texte intégral)\n")
                    .append("Sitemap: ").append(site.url("/sitemap.xml")).append("\n");
        } else {
            t.append("# Ce site n'est volontairement pas référencé : chaque page porte une directive noindex.\n")
                    .append("# L'exploration n'est pas interdite afin que les robots puissent lire cette directive\n")
                    .append("# et que les aperçus de partage (WhatsApp, Facebook, LinkedIn...) continuent de fonctionner.\n")
                    .append("User-agent: *\nDisallow: /api/\n\n")
                    .append("# Robots d'entraînement et de collecte pour les IA : refusés\n");
            for (String bot : AI_CRAWLERS) t.append("User-agent: ").append(bot).append("\n");
            t.append("Disallow: /\n");
        }
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic()).body(t.toString());
    }
}
