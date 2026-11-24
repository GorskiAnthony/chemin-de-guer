package com.chemindeguer.backend.seo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;

/**
 * Récupère la page HTML de base de l'application (index.html servi par nginx), mise en cache une minute.
 * Si nginx est momentanément injoignable, la dernière version connue est réutilisée.
 */
@Component
public class ShellProvider {
    private static final Logger log = LoggerFactory.getLogger(ShellProvider.class);
    private static final Duration TTL = Duration.ofMinutes(1);

    private final RestClient client = RestClient.create();
    private final String url;
    private volatile String cached;
    private volatile Instant fetchedAt = Instant.EPOCH;

    public ShellProvider(@Value("${app.seo.shell-url}") String url) {
        this.url = url;
    }

    /** Renvoie null si la page de base n'a jamais pu être récupérée. */
    public String get() {
        if (cached != null && Duration.between(fetchedAt, Instant.now()).compareTo(TTL) < 0) {
            return cached;
        }
        try {
            String html = client.get().uri(url).retrieve().body(String.class);
            if (html != null && html.contains("</head>")) {
                cached = html;
                fetchedAt = Instant.now();
            }
        } catch (Exception e) {
            log.warn("Page de base indisponible ({}) : {}", url, e.getMessage());
        }
        return cached;
    }
}
