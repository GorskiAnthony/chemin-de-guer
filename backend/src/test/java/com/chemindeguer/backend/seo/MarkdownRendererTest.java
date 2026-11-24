package com.chemindeguer.backend.seo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownRendererTest {
    private final MarkdownRenderer md = new MarkdownRenderer();

    @Test
    void leHtmlBrutEstEchappe() {
        String html = md.toHtml("Bonjour <script>alert(1)</script> <img src=x onerror=alert(1)>");
        assertFalse(html.contains("<script>"));
        assertFalse(html.contains("<img"));
        assertTrue(html.contains("&lt;script&gt;"));
    }

    @Test
    void lesTitresSontDecalesD1Niveau() {
        String html = md.toHtml("# Titre\n\n## Sous-titre");
        assertTrue(html.contains("<h2>Titre</h2>"));
        assertTrue(html.contains("<h3>Sous-titre</h3>"));
        assertFalse(html.contains("<h1>"));
    }

    @Test
    void separateursListesEtGrasItalique() {
        String html = md.toHtml("avant\n\n---\n\n- un\n- deux\n\n***pourquoi eux ?***");
        assertTrue(html.contains("<hr"));
        assertTrue(html.contains("<li>un</li>"));
        assertTrue(html.contains("<strong><em>pourquoi eux ?</em></strong>") || html.contains("<em><strong>pourquoi eux ?</strong></em>"));
    }

    @Test
    void contenuVideDonneUneChaineVide() {
        assertEquals("", md.toHtml(null));
        assertEquals("", md.toHtml("  "));
    }
}
