package com.chemindeguer.backend.seo;

import com.chemindeguer.backend.article.Article;
import com.chemindeguer.backend.article.ArticleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Sitemap (issue #11) et flux RSS (issue #30), exposés via nginx sous /sitemap.xml et /rss.xml. */
@RestController
public class SeoController {
    private final ArticleRepository articleRepository;
    private final String baseUrl;

    public SeoController(ArticleRepository articleRepository, @Value("${app.public-url:http://localhost}") String baseUrl) {
        this.articleRepository = articleRepository;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    @GetMapping(value = "/api/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<String> sitemap() {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n")
                .append("  <url><loc>").append(baseUrl).append("/</loc></url>\n");
        for (Article a : articleRepository.findAllPublished()) {
            xml.append("  <url><loc>").append(baseUrl).append("/articles/").append(a.getSlug() != null ? a.getSlug() : String.valueOf(a.getId())).append("</loc><lastmod>")
                    .append(DateTimeFormatter.ISO_LOCAL_DATE.format(a.getUpdatedAt().atZone(ZoneOffset.UTC)))
                    .append("</lastmod></url>\n");
        }
        return ResponseEntity.ok(xml.append("</urlset>").toString());
    }

    @GetMapping(value = "/api/rss.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<String> rss() {
        List<Article> articles = articleRepository.findAllPublished().stream().limit(20).toList();
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<rss version=\"2.0\"><channel>\n")
                .append("<title>Chemin de Guer</title><link>").append(baseUrl).append("/</link>")
                .append("<description>Journal d'un parcours de conversion au judaïsme</description>\n");
        for (Article a : articles) {
            xml.append("<item><title>").append(HtmlUtils.htmlEscape(a.getTitle())).append("</title><link>")
                    .append(baseUrl).append("/articles/").append(a.getSlug() != null ? a.getSlug() : String.valueOf(a.getId())).append("</link><guid>")
                    .append(baseUrl).append("/articles/").append(a.getSlug() != null ? a.getSlug() : String.valueOf(a.getId())).append("</guid><pubDate>")
                    .append(DateTimeFormatter.RFC_1123_DATE_TIME.format(a.getPublishedAt().atZone(ZoneOffset.UTC)))
                    .append("</pubDate></item>\n");
        }
        return ResponseEntity.ok(xml.append("</channel></rss>").toString());
    }
}
