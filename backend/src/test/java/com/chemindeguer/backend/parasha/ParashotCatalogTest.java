package com.chemindeguer.backend.parasha;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ParashotCatalogTest {
    @Test
    void contientLes54ParashiotEtLes7ParachaDoubles() {
        assertEquals(61, ParashotCatalog.ENTRIES.size());
    }

    @Test
    void chaqueEntreeAUnNomHebreuEtUnSlugUnique() {
        for (var e : ParashotCatalog.ENTRIES) {
            assertNotNull(e.hebrew(), e.name());
            assertFalse(e.hebrew().isBlank(), e.name());
        }
        var noms = ParashotCatalog.ENTRIES.stream().map(ParashotCatalog.Entry::name).collect(Collectors.toList());
        assertEquals(noms.size(), new HashSet<>(noms).size());
    }

    @Test
    void retrouveUneParashaParSlugSansTenirCompteDeLaCasse() {
        var entry = ParashotCatalog.find("  VAYISHLACH ");
        assertNotNull(entry);
        assertEquals("Berechit", entry.book());
        assertEquals("וישלח", entry.hebrew());
        assertTrue(entry.position() < ParashotCatalog.find("Shemot").position());
    }

    @Test
    void lesApostrophesSimpleEtTypographiqueDonnentLaMemeParasha() {
        var typographique = ParashotCatalog.find("Beha’alotcha");
        var simple = ParashotCatalog.find("Beha'alotcha");
        assertNotNull(typographique);
        assertSame(typographique, simple);
        assertEquals("בהעלתך", typographique.hebrew());
    }

    @Test
    void lesParachaDoublesSontReconnues() {
        assertEquals("ויקהל־פקודי", ParashotCatalog.find("Vayakhel-Pekudei").hebrew());
        assertEquals("Vayikra", ParashotCatalog.find("Achrei Mot-Kedoshim").book());
    }

    @Test
    void parashaInconnueRenvoieNull() {
        assertNull(ParashotCatalog.find("Inconnue"));
    }
}
