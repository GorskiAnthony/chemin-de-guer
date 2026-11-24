package com.chemindeguer.backend.fete;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class FetesCatalogTest {

    @Test
    void lesSlugsSontUniques() {
        var slugs = FetesCatalog.ENTRIES.stream().map(FetesCatalog.Entry::slug).collect(Collectors.toList());
        assertEquals(slugs.size(), new HashSet<>(slugs).size());
    }

    @Test
    void lesCategoriesSontContigues() {
        var vues = new LinkedHashSet<String>();
        String precedente = null;
        for (var e : FetesCatalog.ENTRIES) {
            if (!e.category().equals(precedente)) {
                assertTrue(vues.add(e.category()), "catégorie éclatée : " + e.category());
                precedente = e.category();
            }
        }
    }

    @Test
    void normaliseLesNomsHebcal() {
        assertEquals("rosh hashana", FetesCatalog.normalizeKey("Rosh Hashana 5788"));
        assertEquals("chanukah", FetesCatalog.normalizeKey("Chanukah: 3 Candles"));
        assertEquals("sukkot", FetesCatalog.normalizeKey("Sukkot VII (Hoshana Raba)"));
        assertEquals("pesach", FetesCatalog.normalizeKey("Pesach III (CH''M)"));
        assertEquals("rosh chodesh", FetesCatalog.normalizeKey("Rosh Chodesh Tevet"));
        assertEquals("asara b'tevet", FetesCatalog.normalizeKey("Asara B'Tevet"));
        assertEquals("pesach sheni", FetesCatalog.normalizeKey("Pesach Sheni"));
    }

    @Test
    void relieLesNomsHebcalAuxFetesDuCatalogue() {
        assertEquals("Hanoucca", FetesCatalog.matchHebcal("Chanukah: 1 Candle").orElseThrow().name());
        assertEquals("Pessah", FetesCatalog.matchHebcal("Erev Pesach").orElseThrow().name());
        assertEquals("Pourim", FetesCatalog.matchHebcal("Shushan Purim").orElseThrow().name());
        assertEquals("Roch 'Hodech", FetesCatalog.matchHebcal("Rosh Chodesh Sivan").orElseThrow().name());
        assertEquals("Tich'a BeAv", FetesCatalog.matchHebcal("Erev Tish'a B'Av").orElseThrow().name());
    }

    @Test
    void ignoreLesJoursHorsCatalogue() {
        assertTrue(FetesCatalog.matchHebcal("Pesach Sheni").isEmpty());
        assertTrue(FetesCatalog.matchHebcal("Leil Selichot").isEmpty());
        assertTrue(FetesCatalog.matchHebcal("Chag HaBanot").isEmpty());
    }

    @Test
    void trouveUneFeteParSonNomSansTenirCompteDesAccentsNiDeLaCasse() {
        assertEquals("Chemini Atséret", FetesCatalog.find("  chemini atseret ").name());
        assertNull(FetesCatalog.find("Fête inconnue"));
    }

    @Test
    void laCleDePremierRangSertAuCalculDeDate() {
        assertEquals("pesach", FetesCatalog.find("Pessah").primaryKey());
        assertEquals("purim", FetesCatalog.find("Pourim").primaryKey());
    }
}
