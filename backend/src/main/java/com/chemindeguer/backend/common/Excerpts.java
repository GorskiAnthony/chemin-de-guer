package com.chemindeguer.backend.common;

/** Extraits de texte pour les descriptions (balises meta, cartes, RSS) à partir de contenu Markdown. */
public final class Excerpts {
    private static final int MIN_BLOCK = 30;

    private Excerpts() {
    }

    /**
     * Premier vrai paragraphe du texte : ignore titres, citations, séparateurs, tableaux et blocs de code,
     * ainsi que les phrases d'accroche très courtes (« Bonjour à tous ! »). Tronqué proprement à {@code max} caractères.
     */
    public static String of(String markdown, int max) {
        if (markdown == null || markdown.isBlank()) return "";
        String chosen = null;
        for (String block : markdown.split("\\r?\\n\\s*\\r?\\n")) {
            String b = block.trim();
            if (b.isEmpty() || b.startsWith("#") || b.startsWith(">") || b.startsWith("|") || b.startsWith("```")
                    || b.matches("^[-*_]{3,}$")) {
                continue;
            }
            String plain = plain(b);
            if (plain.length() >= MIN_BLOCK) {
                chosen = plain;
                break;
            }
        }
        if (chosen == null) chosen = plain(markdown);
        return truncate(chosen, max);
    }

    static String plain(String markdown) {
        return markdown
                .replaceAll("(?m)^\\s{0,3}#{1,6}\\s+", "")
                .replaceAll("(?m)^\\s*>\\s?", "")
                .replaceAll("(?m)^\\s*([-*+]|\\d+\\.)\\s+", "")
                .replaceAll("!?\\[([^\\]]*)\\]\\([^)]*\\)", "$1")
                .replaceAll("(?m)^\\s*[-*_]{3,}\\s*$", " ")
                .replaceAll("[*_`~]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    static String truncate(String text, int max) {
        if (text.length() <= max) return text;
        int cut = text.lastIndexOf(' ', max - 1);
        return text.substring(0, cut > max / 2 ? cut : max - 1).replaceAll("[\\s,;:.\\-–]+$", "") + "…";
    }
}
