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
    private final boolean indexable;

    public SiteProperties(@Value("${app.public-url:http://localhost}") String baseUrl,
                          @Value("${app.site.name:Chemin de Guer}") String name,
                          @Value("${app.site.description:Journal d'un parcours de conversion au judaïsme : témoignages, étapes et résumés de paracha, montée après montée.}") String description,
                          @Value("${app.author.name:}") String authorName,
                          @Value("${app.author.url:}") String authorUrl,
                          @Value("${app.seo.indexable:false}") boolean indexable) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.name = name;
        this.description = description;
        this.authorName = authorName;
        this.authorUrl = authorUrl;
        this.indexable = indexable;
    }

    public String baseUrl() { return baseUrl; }
    public String name() { return name; }
    public String description() { return description; }
    public String authorName() { return authorName; }
    public String authorUrl() { return authorUrl; }

    /** Faux : le site demande à ne pas être indexé (noindex, robots.txt restrictif, sitemap et llms.txt retirés). */
    public boolean indexable() { return indexable; }

    public String url(String path) { return baseUrl + path; }
}
