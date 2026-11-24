package com.chemindeguer.backend.parasha;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParashotCatalogTest {
    @Test
    void contientLes54Parashiot() {
        assertEquals(54, ParashotCatalog.ENTRIES.size());
    }

    @Test
    void retrouveUneParashaParSlugSansTenirCompteDeLaCasse() {
        var entry = ParashotCatalog.find("  VAYISHLACH ");
        assertNotNull(entry);
        assertEquals("Berechit", entry.book());
        assertTrue(entry.position() < ParashotCatalog.find("Shemot").position());
    }

    @Test
    void parashaInconnueRenvoieNull() {
        assertNull(ParashotCatalog.find("Inconnue"));
    }
}
