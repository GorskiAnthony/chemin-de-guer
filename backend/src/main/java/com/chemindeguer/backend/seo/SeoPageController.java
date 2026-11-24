package com.chemindeguer.backend.seo;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/** Appelé par nginx pour les pages publiques : renvoie la page de l'application avec ses balises SEO et son texte. */
@RestController
public class SeoPageController {
    private final SeoPageService service;

    public SeoPageController(SeoPageService service) {
        this.service = service;
    }

    @GetMapping(value = "/api/seo/page", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> page(@RequestParam String path) {
        SeoPage page = service.render(path);
        if (page == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("");
        }
        if (page.redirect() != null) {
            return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).header(HttpHeaders.LOCATION, page.redirect()).build();
        }
        var builder = ResponseEntity.status(page.status());
        if (page.status() == 200) {
            builder.cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic());
        } else {
            builder.cacheControl(CacheControl.noCache());
        }
        return builder.body(page.html());
    }
}
