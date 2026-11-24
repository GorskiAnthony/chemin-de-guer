package com.chemindeguer.backend.seo;

import com.chemindeguer.backend.article.ArticleRepository;
import com.chemindeguer.backend.article.StatutArticle;
import com.chemindeguer.backend.fete.FeteRepository;
import com.chemindeguer.backend.parasha.ParashaRepository;
import com.chemindeguer.backend.tag.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Comparator;

/** Images de partage dynamiques, appelées par nginx sous /og/. */
@RestController
public class OgImageController {
    private final OgImageService images;
    private final ArticleRepository articles;
    private final ParashaRepository parashot;
    private final FeteRepository fetes;

    public OgImageController(OgImageService images, ArticleRepository articles, ParashaRepository parashot, FeteRepository fetes) {
        this.images = images;
        this.articles = articles;
        this.parashot = parashot;
        this.fetes = fetes;
    }

    @GetMapping("/api/og/articles/{file}")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> article(@PathVariable String file) {
        String slug = stripPng(file);
        var found = articles.findPublishedBySlug(slug);
        if (found.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        var a = found.get();
        String kicker = a.getTags().stream().map(Tag::getName).min(Comparator.naturalOrder()).orElse("Journal de conversion");
        return png(images.render("a:" + slug + ":" + a.getUpdatedAt().toEpochMilli(), kicker, a.getTitle(), null));
    }

    @GetMapping("/api/og/paracha/{file}")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> parasha(@PathVariable String file) {
        String slug = stripPng(file);
        var found = parashot.findBySlugAndStatut(slug, StatutArticle.PUBLISHED);
        if (found.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        var p = found.get();
        return png(images.render("p:" + slug + ":" + p.getUpdatedAt().toEpochMilli(), "Résumé de paracha · " + p.getBook(),
                "Paracha " + p.getName(), p.getHebrewName()));
    }

    @GetMapping("/api/og/fetes/{file}")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> fete(@PathVariable String file) {
        String slug = stripPng(file);
        var found = fetes.findBySlugAndStatut(slug, StatutArticle.PUBLISHED);
        if (found.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        var f = found.get();
        return png(images.render("f:" + slug + ":" + f.getUpdatedAt().toEpochMilli(), "Fête · " + f.getCategory(), f.getName(), f.getHebrewName()));
    }

    private static String stripPng(String file) {
        return file.endsWith(".png") ? file.substring(0, file.length() - 4) : file;
    }

    private static ResponseEntity<byte[]> png(byte[] bytes) {
        if (bytes == null) { // rendu indisponible : image par défaut
            return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, "/og-default.png").build();
        }
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(Duration.ofHours(6)).cachePublic()).body(bytes);
    }
}
