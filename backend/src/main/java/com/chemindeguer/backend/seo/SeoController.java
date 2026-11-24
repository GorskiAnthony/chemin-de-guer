package com.chemindeguer.backend.seo;

import com.chemindeguer.backend.article.Article;
import com.chemindeguer.backend.article.ArticleRepository;
import com.chemindeguer.backend.article.StatutArticle;
import com.chemindeguer.backend.parasha.Aliyah;
import com.chemindeguer.backend.parasha.ParashaRepository;
import com.chemindeguer.backend.parasha.ParashaSummary;
import com.chemindeguer.backend.tag.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static com.chemindeguer.backend.seo.Html.esc;

/**
 * Sitemap, flux RSS complet et fichiers destinés aux moteurs de réponse (llms.txt, llms-full.txt).
 * nginx les expose sous /sitemap.xml, /rss.xml, /llms.txt et /llms-full.txt.
 */
@RestController
public class SeoController {
    private static final DateTimeFormatter FR_DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH).withZone(ZoneOffset.UTC);

    private final ArticleRepository articles;
    private final ParashaRepository parashot;
    private final SiteProperties site;
    private final MarkdownRenderer markdown;

    public SeoController(ArticleRepository articles, ParashaRepository parashot, SiteProperties site, MarkdownRenderer markdown) {
        this.articles = articles;
        this.parashot = parashot;
        this.site = site;
        this.markdown = markdown;
    }

    @GetMapping(value = "/api/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<String> sitemap() {
        if (!site.indexable()) return ResponseEntity.notFound().build();
        List<Article> arts = articles.findAllPublished();
        List<ParashaSummary> pars = parashot.findAllByStatutOrderByPositionAsc(StatutArticle.PUBLISHED);
        Instant lastArticle = arts.stream().map(Article::getUpdatedAt).max(Comparator.naturalOrder()).orElse(null);
        Instant lastParasha = pars.stream().map(ParashaSummary::getUpdatedAt).max(Comparator.naturalOrder()).orElse(null);
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        url(xml, "/", latest(lastArticle, lastParasha));
        url(xml, "/articles", lastArticle);
        url(xml, "/paracha", lastParasha);
        for (Article a : arts) url(xml, "/articles/" + (a.getSlug() != null ? a.getSlug() : a.getId()), a.getUpdatedAt());
        for (ParashaSummary p : pars) url(xml, "/paracha/" + p.getSlug(), p.getUpdatedAt());
        return xmlResponse(xml.append("</urlset>").toString());
    }

    @GetMapping(value = "/api/rss.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @Transactional(readOnly = true)
    public ResponseEntity<String> rss() {
        List<Article> arts = articles.findAllPublished().stream().limit(30).toList();
        String last = arts.isEmpty() ? null : rfc822(arts.stream().map(Article::getUpdatedAt).max(Comparator.naturalOrder()).get());
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<rss version=\"2.0\" xmlns:atom=\"http://www.w3.org/2005/Atom\" xmlns:content=\"http://purl.org/rss/1.0/modules/content/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\"><channel>\n")
                .append("<title>").append(esc(site.name())).append("</title><link>").append(esc(site.url("/"))).append("</link>")
                .append("<description>").append(esc(site.description())).append("</description><language>fr-FR</language>\n")
                .append("<atom:link href=\"").append(esc(site.url("/rss.xml"))).append("\" rel=\"self\" type=\"application/rss+xml\"/>\n");
        if (last != null) xml.append("<lastBuildDate>").append(last).append("</lastBuildDate>\n");
        for (Article a : arts) {
            String link = site.url("/articles/" + (a.getSlug() != null ? a.getSlug() : a.getId()));
            xml.append("<item><title>").append(esc(a.getTitle())).append("</title><link>").append(esc(link)).append("</link>")
                    .append("<guid isPermaLink=\"true\">").append(esc(link)).append("</guid>")
                    .append("<pubDate>").append(rfc822(a.getPublishedAt())).append("</pubDate>")
                    .append("<dc:creator>").append(esc(site.authorName())).append("</dc:creator>")
                    .append("<description>").append(esc(SeoPageService.description(a))).append("</description>");
            for (Tag t : a.getTags()) xml.append("<category>").append(esc(t.getName())).append("</category>");
            xml.append("<content:encoded><![CDATA[").append(markdown.toHtml(a.getContent()).replace("]]>", "]]]]><![CDATA[>"))
                    .append("]]></content:encoded></item>\n");
        }
        return xmlResponse(xml.append("</channel></rss>").toString());
    }

    @GetMapping(value = "/api/llms.txt", produces = "text/plain;charset=UTF-8")
    @Transactional(readOnly = true)
    public ResponseEntity<String> llms() {
        if (!site.indexable()) return ResponseEntity.notFound().build();
        StringBuilder t = new StringBuilder("# ").append(site.name()).append("\n\n> ").append(site.description()).append("\n\n")
                .append("Journal personnel écrit en français par ").append(site.authorName()).append(" (").append(site.authorUrl())
                .append("). « Guer » (גר) désigne en hébreu le converti. Le site contient des articles de témoignage et des résumés de la paracha ")
                .append("(lecture hebdomadaire de la Torah), rédigés montée par montée.\n\n## Articles\n\n");
        for (Article a : articles.findAllPublished()) {
            t.append("- [").append(a.getTitle()).append("](").append(site.url("/articles/" + a.getSlug())).append("): ")
                    .append(SeoPageService.description(a)).append("\n");
        }
        t.append("\n## Paracha\n\n");
        for (ParashaSummary p : parashot.findAllByStatutOrderByPositionAsc(StatutArticle.PUBLISHED)) {
            t.append("- [Paracha ").append(p.getName()).append("](").append(site.url("/paracha/" + p.getSlug())).append(")")
                    .append(p.getHebrewName() != null ? " (" + p.getHebrewName() + ")" : "").append(", ").append(p.getBook()).append("\n");
        }
        t.append("\n## Pour aller plus loin\n\n- [Texte intégral de tous les contenus](").append(site.url("/llms-full.txt")).append(")\n")
                .append("- [Flux RSS](").append(site.url("/rss.xml")).append(")\n")
                .append("- [Plan du site](").append(site.url("/sitemap.xml")).append(")\n");
        return textResponse(t.toString());
    }

    @GetMapping(value = "/api/llms-full.txt", produces = "text/plain;charset=UTF-8")
    @Transactional(readOnly = true)
    public ResponseEntity<String> llmsFull() {
        if (!site.indexable()) return ResponseEntity.notFound().build();
        StringBuilder t = new StringBuilder("# ").append(site.name()).append(" : texte intégral\n\n> ").append(site.description()).append("\n\n");
        for (Article a : articles.findAllPublished()) {
            t.append("---\n\n# ").append(a.getTitle()).append("\n\nSource : ").append(site.url("/articles/" + a.getSlug()))
                    .append("\nAuteur : ").append(site.authorName());
            if (a.getPublishedAt() != null) t.append("\nPublié le ").append(FR_DATE.format(a.getPublishedAt()));
            t.append("\n\n").append(a.getContent()).append("\n\n");
        }
        for (ParashaSummary p : parashot.findAllByStatutOrderByPositionAsc(StatutArticle.PUBLISHED)) {
            t.append("---\n\n# Paracha ").append(p.getName()).append(p.getHebrewName() != null ? " (" + p.getHebrewName() + ")" : "")
                    .append("\n\nSource : ").append(site.url("/paracha/" + p.getSlug())).append("\nAuteur : ").append(site.authorName()).append("\n\n");
            if (p.getIntro() != null) t.append(p.getIntro()).append("\n\n");
            int i = 1;
            for (Aliyah al : p.getAliyot()) {
                t.append("## ").append(al.getLabel() == null || al.getLabel().isBlank() ? "Montée " + i : al.getLabel()).append("\n\n")
                        .append(al.getContent()).append("\n\n");
                i++;
            }
        }
        return textResponse(t.toString());
    }

    // ------------------------------------------------------------------ utilitaires

    private void url(StringBuilder xml, String path, Instant lastmod) {
        xml.append("  <url><loc>").append(esc(site.url(path))).append("</loc>");
        if (lastmod != null) xml.append("<lastmod>").append(lastmod.atZone(ZoneOffset.UTC).toLocalDate()).append("</lastmod>");
        xml.append("</url>\n");
    }

    private static Instant latest(Instant a, Instant b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }

    private static String rfc822(Instant i) {
        return DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.ofInstant(i, ZoneOffset.UTC));
    }

    private static ResponseEntity<String> xmlResponse(String body) {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic()).body(body);
    }

    private static ResponseEntity<String> textResponse(String body) {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic()).body(body);
    }
}
