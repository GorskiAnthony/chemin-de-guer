package com.chemindeguer.backend.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SlugsTest {
    @Test
    void retireAccentsEtSymboles() {
        assertEquals("fetes-juives", Slugs.of("  Fêtes Juives ! "));
    }

    @Test
    void valeurParDefautSiVide() {
        assertEquals("tag", Slugs.of("!!!"));
    }

    @Test
    void slugDArticleNeEstJamaisNumerique() {
        assertEquals("article-2026", Slugs.forArticle("2026"));
        assertEquals("article", Slugs.forArticle("!!!"));
        assertEquals("presentation", Slugs.forArticle("Présentation"));
    }

    @Test
    void slugDArticleEstTronque() {
        String slug = Slugs.forArticle("mot ".repeat(60));
        assertTrue(slug.length() <= 80);
        assertFalse(slug.endsWith("-"));
    }
}
