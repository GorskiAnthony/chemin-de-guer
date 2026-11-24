package com.chemindeguer.backend.shabbat;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ShabbatWindowTest {

    private final ShabbatWindow window = new ShabbatWindow(
            Instant.parse("2026-11-27T15:10:00Z"), Instant.parse("2026-11-28T17:00:00Z"));

    @Test
    void contientLesInstantsDansLaFenetre() {
        assertTrue(window.contains(Instant.parse("2026-11-27T15:10:00Z")));
        assertTrue(window.contains(Instant.parse("2026-11-28T12:00:00Z")));
    }

    @Test
    void exclutLesInstantsHorsFenetre() {
        assertFalse(window.contains(Instant.parse("2026-11-27T15:09:59Z")));
        assertFalse(window.contains(Instant.parse("2026-11-28T17:00:00Z")));
    }

    @Test
    void vendrediDeReference() {
        assertEquals(LocalDate.of(2026, 11, 27), ShabbatWindow.referenceFriday(LocalDate.of(2026, 11, 24))); // mardi
        assertEquals(LocalDate.of(2026, 11, 27), ShabbatWindow.referenceFriday(LocalDate.of(2026, 11, 27))); // vendredi
        assertEquals(LocalDate.of(2026, 11, 27), ShabbatWindow.referenceFriday(LocalDate.of(2026, 11, 28))); // samedi
        assertEquals(LocalDate.of(2026, 12, 4), ShabbatWindow.referenceFriday(LocalDate.of(2026, 11, 29)));  // dimanche
    }
}
