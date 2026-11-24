package com.chemindeguer.backend.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExcerptsTest {
    private static final String ARTICLE = """
            # Mon chemin vers le judaïsme

            > *« Ton peuple sera mon peuple. »* (Ruth 1:16)

            Bonjour à tous !

            Moi, c'est Anthony et je tiens ce **journal** pour raconter ma conversion, étape après étape.

            ---

            ## Genèse

            Un autre paragraphe.
            """;

    @Test
    void ignoreTitreCitationEtAccrocheCourte() {
        assertEquals("Moi, c'est Anthony et je tiens ce journal pour raconter ma conversion, étape après étape.", Excerpts.of(ARTICLE, 200));
    }

    @Test
    void tronqueProprementALaLimite() {
        String e = Excerpts.of("Un paragraphe suffisamment long pour devoir être coupé à la limite demandée sans couper un mot.", 40);
        assertTrue(e.endsWith("…"));
        assertTrue(e.length() <= 41);
        assertFalse(e.contains("couper un mo "));
    }

    @Test
    void retombeSurLeTexteCompletSiAucunParagrapheNeConvient() {
        assertEquals("Titre seul", Excerpts.of("# Titre seul", 100));
    }

    @Test
    void vide() {
        assertEquals("", Excerpts.of(null, 100));
        assertEquals("", Excerpts.of("   ", 100));
    }
}
