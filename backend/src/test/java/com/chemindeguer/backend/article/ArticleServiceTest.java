package com.chemindeguer.backend.article;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArticleServiceTest {

    @Test
    void requeteVideDonneUnMotifQuiToutAccepte() {
        assertEquals("%", ArticleService.toLikePattern(null));
        assertEquals("%", ArticleService.toLikePattern("   "));
    }

    @Test
    void requeteEstEnMinusculesEtEchappee() {
        assertEquals("%100\\% sûr\\_%", ArticleService.toLikePattern(" 100% SÛR_ "));
    }

    @Test
    void extraitTronqueLesContenusLongs() {
        String longText = "mot ".repeat(200);
        String excerpt = ArticleResponse.excerpt(longText);
        assertTrue(excerpt.endsWith("…"));
        assertTrue(excerpt.length() <= 221);
    }
}
