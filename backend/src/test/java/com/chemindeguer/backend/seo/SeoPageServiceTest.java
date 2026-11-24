package com.chemindeguer.backend.seo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SeoPageServiceTest {
    @Test
    void normaliseLesChemins() {
        assertEquals("/", SeoPageService.normalize(null));
        assertEquals("/", SeoPageService.normalize("/"));
        assertEquals("/articles", SeoPageService.normalize("/articles/"));
        assertEquals("/articles/mon-slug", SeoPageService.normalize("/articles/mon-slug?utm=1#x"));
        assertEquals("/paracha", SeoPageService.normalize("paracha"));
    }

    @Test
    void extraitSansMarkdownEtLimiteA160Caracteres() {
        String md = "# Titre\n\n> *« Citation »* (Ruth 1:16)\n\n---\n\n- liste **grasse** avec [lien](https://exemple.fr)\n\n" + "mot ".repeat(80);
        String e = SeoPageService.excerpt(md);
        assertTrue(e.length() <= 161, "longueur " + e.length());
        assertFalse(e.contains("#"));
        assertFalse(e.contains("**"));
        assertFalse(e.contains("---"));
        assertFalse(e.contains("https://"));
        assertTrue(e.endsWith("…"));
    }

    @Test
    void extraitCourtInchange() {
        assertEquals("Un texte court.", SeoPageService.excerpt("Un texte court."));
    }
}
