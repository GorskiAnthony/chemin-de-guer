package com.chemindeguer.backend.stats;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

/** Règles de tri des visites : quelles pages comptent, quels visiteurs sont ignorés, ce qu'on retient du site d'origine. */
final class HitRules {
    private static final Pattern PATH = Pattern.compile("^/$|^/(articles|paracha|fetes)$|^/(articles|paracha|fetes)/[a-z0-9-]{1,100}$");
    private static final Pattern BOT = Pattern.compile(
            "bot|crawl|spider|slurp|preview|facebookexternalhit|whatsapp|telegram|curl|wget|python|headless|monitor|uptime|lighthouse|httpclient",
            Pattern.CASE_INSENSITIVE);

    private HitRules() {
    }

    /** Chemin public normalisé (sans paramètres ni barre finale), ou null si la page ne doit pas être comptée. */
    static String normalizePath(String raw) {
        if (raw == null) return null;
        String p = raw;
        int cut = Math.min(p.indexOf('?') < 0 ? p.length() : p.indexOf('?'), p.indexOf('#') < 0 ? p.length() : p.indexOf('#'));
        p = p.substring(0, cut);
        if (p.length() > 1 && p.endsWith("/")) p = p.substring(0, p.length() - 1);
        return PATH.matcher(p).matches() ? p : null;
    }

    static boolean isBot(String userAgent) {
        return userAgent == null || userAgent.isBlank() || BOT.matcher(userAgent).find();
    }

    static String device(String userAgent) {
        String ua = userAgent.toLowerCase(Locale.ROOT);
        if (ua.contains("ipad") || ua.contains("tablet")) return "tablette";
        if (ua.contains("mobi") || ua.contains("android") || ua.contains("iphone")) return "mobile";
        return "ordinateur";
    }

    /** Seul le nom de domaine du site d'origine est conservé, jamais l'adresse complète. Null pour un accès direct ou interne. */
    static String referrerHost(String referrer, String ownHost) {
        if (referrer == null || referrer.isBlank() || referrer.length() > 500) return null;
        try {
            String host = URI.create(referrer.trim()).getHost();
            if (host == null) return null;
            host = host.toLowerCase(Locale.ROOT).replaceFirst("^www\\.", "");
            if (host.length() > 100) return null;
            String own = ownHost == null ? "" : ownHost.toLowerCase(Locale.ROOT).replaceFirst("^www\\.", "");
            return host.equals(own) ? null : host;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
