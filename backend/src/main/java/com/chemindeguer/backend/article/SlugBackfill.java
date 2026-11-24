package com.chemindeguer.backend.article;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Au démarrage, génère le slug des articles qui n'en ont pas encore (créés avant l'introduction des slugs). */
@Component
public class SlugBackfill implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SlugBackfill.class);
    private final ArticleService articleService;

    public SlugBackfill(ArticleService articleService) {
        this.articleService = articleService;
    }

    @Override
    public void run(ApplicationArguments args) {
        int n = articleService.backfillSlugs();
        if (n > 0) log.info("Slug généré pour {} article(s).", n);
    }
}
