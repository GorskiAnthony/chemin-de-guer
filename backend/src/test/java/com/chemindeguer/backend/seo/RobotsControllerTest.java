package com.chemindeguer.backend.seo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RobotsControllerTest {
    private static String robots(boolean indexable) {
        var site = new SiteProperties("https://exemple.fr", "Site", "Desc", "Auteur", "https://auteur.fr", indexable);
        return new RobotsController(site).robots().getBody();
    }

    @Test
    void siteNonIndexableRefuseLesRobotsIAEtNAnnoncePasDeSitemap() {
        String t = robots(false);
        assertTrue(t.contains("User-agent: GPTBot"));
        assertTrue(t.contains("Disallow: /\n"));
        assertFalse(t.contains("Sitemap:"));
        assertFalse(t.contains("llms.txt"));
    }

    @Test
    void siteNonIndexableNInterditPasLExplorationPourLaisserLireLeNoindex() {
        String t = robots(false);
        // le groupe « * » n'interdit que l'API : les robots doivent pouvoir lire la directive noindex des pages
        String groupeEtoile = t.substring(t.indexOf("User-agent: *"), t.indexOf("# Robots d'entra"));
        assertTrue(groupeEtoile.contains("Disallow: /api/"));
        assertFalse(groupeEtoile.contains("Disallow: /\n"));
    }

    @Test
    void siteIndexableOuvreLeContenuEtAnnonceLeSitemap() {
        String t = robots(true);
        assertTrue(t.contains("Sitemap: https://exemple.fr/sitemap.xml"));
        assertTrue(t.contains("Allow: /"));
        assertFalse(t.contains("Disallow: /\n"));
    }
}
