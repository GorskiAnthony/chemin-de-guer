package com.chemindeguer.backend.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SlugsTest {
    @Test
    void retireAccentsEtSymboles() {
        assertEquals("fetes-juives", Slugs.of("  Fêtes Juives ! "));
    }

    @Test
    void valeurParDefautSiVide() {
        assertEquals("tag", Slugs.of("!!!"));
    }
}
