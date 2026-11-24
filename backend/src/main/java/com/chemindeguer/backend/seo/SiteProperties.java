package com.chemindeguer.backend.seo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Identité du site, utilisée pour les balises SEO, les données structurées et llms.txt. */
@Component
public class SiteProperties {
    private final String baseUrl;
    private final String name;
    private final String description;
    private final String authorName;
    private final String authorUrl;

    public SiteProperties(@Value("${app.public-url:http://localhost}") String baseUrl,
                          @Value("${app.site.name:Chemin de Guer}") String name,
                          @Value("${app.site.description:Journal d'un parcours de conversion au judaïsme : témoignages, étapes et résumés de paracha, montée après montée.}") String description,
                          @Value("${app.author.name:}") String authorName,
                          @Value("${app.author.url:}") String authorUrl) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.name = name;
        this.description = description;
        this.authorName = authorName;
        this.authorUrl = authorUrl;
    }

    public String baseUrl() { return baseUrl; }
    public String name() { return name; }
    public String description() { return description; }
    public String authorName() { return authorName; }
    public String authorUrl() { return authorUrl; }

    public String url(String path) { return baseUrl + path; }
}
