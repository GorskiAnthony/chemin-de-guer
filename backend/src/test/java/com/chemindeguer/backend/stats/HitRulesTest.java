package com.chemindeguer.backend.stats;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class HitRulesTest {
    @Test
    void normalizePathKeepsOnlyPublicPages() {
        assertEquals("/", HitRules.normalizePath("/"));
        assertEquals("/articles", HitRules.normalizePath("/articles/"));
        assertEquals("/articles/mon-slug", HitRules.normalizePath("/articles/mon-slug?utm_source=x#haut"));
        assertEquals("/paracha/beha-alotcha", HitRules.normalizePath("/paracha/beha-alotcha"));
        assertNull(HitRules.normalizePath("/admin"));
        assertNull(HitRules.normalizePath("/admin/login"));
        assertNull(HitRules.normalizePath("/articles/../admin"));
        assertNull(HitRules.normalizePath("/articles/Mon Slug"));
        assertNull(HitRules.normalizePath("https://evil.example/"));
        assertNull(HitRules.normalizePath(null));
    }

    @Test
    void botsAreIgnored() {
        assertTrue(HitRules.isBot(null));
        assertTrue(HitRules.isBot(""));
        assertTrue(HitRules.isBot("Mozilla/5.0 (compatible; Googlebot/2.1)"));
        assertTrue(HitRules.isBot("facebookexternalhit/1.1"));
        assertTrue(HitRules.isBot("curl/8.4.0"));
        assertFalse(HitRules.isBot("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Version/17 Safari/605.1.15"));
    }

    @Test
    void deviceIsGuessedFromUserAgent() {
        assertEquals("mobile", HitRules.device("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0) Mobile/15E148"));
        assertEquals("tablette", HitRules.device("Mozilla/5.0 (iPad; CPU OS 17_0)"));
        assertEquals("ordinateur", HitRules.device("Mozilla/5.0 (Windows NT 10.0; Win64; x64)"));
    }

    @Test
    void referrerKeepsOnlyTheExternalHost() {
        assertEquals("google.com", HitRules.referrerHost("https://www.google.com/search?q=secret", "chemin-de-guer.fr"));
        assertNull(HitRules.referrerHost("https://chemin-de-guer.fr/articles/x", "chemin-de-guer.fr"));
        assertNull(HitRules.referrerHost("https://www.chemin-de-guer.fr/", "chemin-de-guer.fr"));
        assertNull(HitRules.referrerHost("", "chemin-de-guer.fr"));
        assertNull(HitRules.referrerHost("pas une adresse", "chemin-de-guer.fr"));
    }

    @Test
    void visitorHashIsStableInADayAndDoesNotContainTheIp() {
        var hasher = new VisitorHasher();
        LocalDate day = LocalDate.of(2026, 11, 25);
        String a = hasher.hash(day, "203.0.113.7", "UA");
        assertEquals(a, hasher.hash(day, "203.0.113.7", "UA"));
        assertNotEquals(a, hasher.hash(day, "203.0.113.8", "UA"));
        assertFalse(a.contains("203"));
        // Le lendemain, le sel change : le même visiteur n'est plus reconnaissable
        assertNotEquals(a, hasher.hash(day.plusDays(1), "203.0.113.7", "UA"));
    }
}
