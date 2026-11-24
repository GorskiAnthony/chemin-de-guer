package com.chemindeguer.backend.seo;

import com.chemindeguer.backend.article.Article;
import com.chemindeguer.backend.article.ArticleRepository;
import com.chemindeguer.backend.article.StatutArticle;
import com.chemindeguer.backend.calendar.CalendarService;
import com.chemindeguer.backend.common.Excerpts;
import com.chemindeguer.backend.fete.FeteRepository;
import com.chemindeguer.backend.fete.FeteSection;
import com.chemindeguer.backend.fete.FeteSummary;
import com.chemindeguer.backend.parasha.Aliyah;
import com.chemindeguer.backend.parasha.ParashaRepository;
import com.chemindeguer.backend.parasha.ParashaSummary;
import com.chemindeguer.backend.tag.Tag;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

import static com.chemindeguer.backend.seo.Html.esc;

/**
 * Produit, pour chaque page publique, la page HTML de base de l'application enrichie de ce que les robots
 * (moteurs de recherche, réseaux sociaux, IA) ne peuvent pas obtenir en exécutant du JavaScript :
 * balises SEO et de partage, données structurées et texte de la page.
 */
@Service
public class SeoPageService {
    private static final Pattern KEY = Pattern.compile("[a-z0-9-]{1,100}");
    private static final DateTimeFormatter FR_DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH).withZone(ZoneId.of("Europe/Paris"));
    private static final DateTimeFormatter FETE_DATE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
    private static final String OG_DEFAULT = "/og-default.png";
    private static final String ROBOTS_INDEX = "index, follow, max-image-preview:large, max-snippet:-1, max-video-preview:-1";
    static final String ROBOTS_NOINDEX = "noindex, nofollow, noarchive";

    private final ArticleRepository articles;
    private final ParashaRepository parashot;
    private final FeteRepository fetes;
    private final CalendarService calendar;
    private final ShellProvider shell;
    private final MarkdownRenderer markdown;
    private final JsonMapper json;
    private final SiteProperties site;

    public SeoPageService(ArticleRepository articles, ParashaRepository parashot, FeteRepository fetes, CalendarService calendar, ShellProvider shell,
                          MarkdownRenderer markdown, JsonMapper json, SiteProperties site) {
        this.articles = articles;
        this.parashot = parashot;
        this.fetes = fetes;
        this.calendar = calendar;
        this.shell = shell;
        this.markdown = markdown;
        this.json = json;
        this.site = site;
    }

    /** Renvoie null si la page de base de l'application est indisponible (nginx servira alors la page statique). */
    @Transactional(readOnly = true)
    public SeoPage render(String rawPath) {
        String base = shell.get();
        if (base == null) return null;
        String path = normalize(rawPath);
        String[] seg = path.equals("/") ? new String[0] : path.substring(1).split("/");

        if (seg.length == 0) return home(base);
        if (seg.length == 1 && seg[0].equals("articles")) return articleList(base);
        if (seg.length == 1 && seg[0].equals("paracha")) return parashaList(base);
        if (seg.length == 1 && seg[0].equals("fetes")) return feteList(base);
        if (seg.length == 2 && KEY.matcher(seg[1]).matches()) {
            if (seg[0].equals("articles")) return article(base, seg[1]);
            if (seg[0].equals("paracha")) return parasha(base, seg[1]);
            if (seg[0].equals("fetes")) return fete(base, seg[1]);
        }
        return notFound(base);
    }

    // ------------------------------------------------------------------ pages

    private SeoPage home(String base) {
        List<Article> latest = articles.findAllPublished().stream().limit(10).toList();
        Head h = new Head("Chemin de Guer · Journal d'un parcours de conversion au judaïsme", site.description(), "/");
        h.ogType = "website";
        h.jsonLd.add(graph(websiteNode(), personNode(), blogNode(), breadcrumb(List.of(crumb("Accueil", "/")))));
        StringBuilder body = new StringBuilder("<h1>").append(esc(site.name())).append("</h1><p>").append(esc(site.description())).append("</p>");
        body.append("<h2>Derniers articles</h2>").append(articleLinks(latest));
        body.append("<p><a href=\"").append(esc(site.url("/articles"))).append("\">Tous les articles</a> · <a href=\"")
                .append(esc(site.url("/paracha"))).append("\">Résumés de paracha</a> · <a href=\"")
                .append(esc(site.url("/fetes"))).append("\">Fêtes</a></p>");
        return SeoPage.ok(compose(base, h, body.toString()));
    }

    private SeoPage articleList(String base) {
        List<Article> all = articles.findAllPublished();
        Head h = new Head("Articles · Chemin de Guer", "Tous les articles du journal : témoignages et étapes d'un parcours de conversion au judaïsme.", "/articles");
        h.ogType = "website";
        h.jsonLd.add(graph(websiteNode(), personNode(), collectionPage("Articles", h.description, "/articles"),
                breadcrumb(List.of(crumb("Accueil", "/"), crumb("Articles", "/articles")))));
        String body = "<h1>Articles</h1>" + articleLinks(all);
        return SeoPage.ok(compose(base, h, body));
    }

    private SeoPage article(String base, String key) {
        Optional<Article> found = key.matches("\\d{1,18}") ? articles.findPublishedById(Long.parseLong(key)) : articles.findPublishedBySlug(key);
        if (found.isEmpty()) return notFound(base);
        Article a = found.get();
        if (a.getSlug() != null && !a.getSlug().equals(key)) {
            return SeoPage.redirect("/articles/" + a.getSlug()); // ancien lien par identifiant : redirection permanente
        }
        String path = "/articles/" + (a.getSlug() != null ? a.getSlug() : a.getId());
        String excerpt = description(a);
        Head h = new Head(a.getTitle() + " · " + site.name(), excerpt, path);
        h.ogTitle = a.getTitle();
        h.ogType = "article";
        h.image = a.getSlug() != null ? "/og/articles/" + a.getSlug() + ".png" : OG_DEFAULT;
        h.imageAlt = a.getTitle() + " · " + site.name();
        h.published = a.getPublishedAt();
        h.modified = a.getUpdatedAt();
        h.tags = a.getTags().stream().map(Tag::getName).sorted().toList();

        Map<String, Object> post = new LinkedHashMap<>();
        post.put("@type", "BlogPosting");
        post.put("@id", site.url(path) + "#article");
        post.put("headline", a.getTitle());
        post.put("description", excerpt);
        post.put("url", site.url(path));
        post.put("mainEntityOfPage", Map.of("@type", "WebPage", "@id", site.url(path)));
        post.put("datePublished", iso(a.getPublishedAt()));
        post.put("dateModified", iso(a.getUpdatedAt()));
        post.put("inLanguage", "fr-FR");
        post.put("author", Map.of("@id", site.url("/#person")));
        post.put("publisher", Map.of("@id", site.url("/#person")));
        post.put("isPartOf", Map.of("@id", site.url("/#website")));
        post.put("image", List.of(site.url(h.image)));
        post.put("wordCount", wordCount(a.getContent()));
        if (!h.tags.isEmpty()) {
            post.put("keywords", String.join(", ", h.tags));
            post.put("articleSection", h.tags.get(0));
        }
        h.jsonLd.add(graph(websiteNode(), personNode(), post,
                breadcrumb(List.of(crumb("Accueil", "/"), crumb("Articles", "/articles"), crumb(a.getTitle(), path)))));

        StringBuilder body = new StringBuilder("<article><h1>").append(esc(a.getTitle())).append("</h1><p>Par <a href=\"")
                .append(esc(site.authorUrl())).append("\">").append(esc(site.authorName())).append("</a>");
        if (a.getPublishedAt() != null) {
            body.append(" · Publié le <time datetime=\"").append(iso(a.getPublishedAt())).append("\">").append(FR_DATE.format(a.getPublishedAt())).append("</time>");
        }
        body.append("</p>").append(markdown.toHtml(a.getContent())).append("</article>");
        return SeoPage.ok(compose(base, h, body.toString()));
    }

    private SeoPage parashaList(String base) {
        List<ParashaSummary> all = parashot.findAllByStatutOrderByPositionAsc(StatutArticle.PUBLISHED);
        Head h = new Head("Paracha · Chemin de Guer", "Mes résumés de la paracha de la semaine, montée par montée, de Berechit à Devarim.", "/paracha");
        h.ogType = "website";
        h.jsonLd.add(graph(websiteNode(), personNode(), collectionPage("Paracha", h.description, "/paracha"),
                breadcrumb(List.of(crumb("Accueil", "/"), crumb("Paracha", "/paracha")))));
        StringBuilder body = new StringBuilder("<h1>Paracha</h1>");
        String book = null;
        for (ParashaSummary p : all) {
            if (!p.getBook().equals(book)) {
                if (book != null) body.append("</ul>");
                book = p.getBook();
                body.append("<h2>").append(esc(book)).append("</h2><ul>");
            }
            body.append("<li><a href=\"").append(esc(site.url("/paracha/" + p.getSlug()))).append("\">").append(esc(p.getName())).append("</a>");
            if (p.getHebrewName() != null) body.append(" <span lang=\"he\" dir=\"rtl\">").append(esc(p.getHebrewName())).append("</span>");
            body.append("</li>");
        }
        if (book != null) body.append("</ul>");
        return SeoPage.ok(compose(base, h, body.toString()));
    }

    private SeoPage parasha(String base, String slug) {
        Optional<ParashaSummary> found = parashot.findBySlugAndStatut(slug, StatutArticle.PUBLISHED);
        if (found.isEmpty()) return notFound(base);
        ParashaSummary p = found.get();
        String path = "/paracha/" + p.getSlug();
        String intro = Excerpts.of(p.getIntro(), 160);
        String description = intro.isBlank() ? "Mon résumé de la paracha " + p.getName() + ", montée par montée." : intro;
        Head h = new Head("Paracha " + p.getName() + " : résumé par montée · " + site.name(), description, path);
        h.ogTitle = "Paracha " + p.getName();
        h.ogType = "article";
        h.image = "/og/paracha/" + p.getSlug() + ".png";
        h.imageAlt = "Paracha " + p.getName() + " · " + site.name();
        h.modified = p.getUpdatedAt();
        h.published = p.getCreatedAt();
        h.tags = List.of("Paracha", p.getBook());

        Map<String, Object> article = new LinkedHashMap<>();
        article.put("@type", "Article");
        article.put("@id", site.url(path) + "#article");
        article.put("headline", "Paracha " + p.getName() + " : résumé par montée");
        article.put("description", description);
        article.put("url", site.url(path));
        article.put("mainEntityOfPage", Map.of("@type", "WebPage", "@id", site.url(path)));
        article.put("datePublished", iso(p.getCreatedAt()));
        article.put("dateModified", iso(p.getUpdatedAt()));
        article.put("inLanguage", "fr-FR");
        article.put("about", p.getName() + (p.getHebrewName() != null ? " (" + p.getHebrewName() + ")" : ""));
        article.put("author", Map.of("@id", site.url("/#person")));
        article.put("publisher", Map.of("@id", site.url("/#person")));
        article.put("isPartOf", Map.of("@id", site.url("/#website")));
        article.put("image", List.of(site.url(h.image)));
        h.jsonLd.add(graph(websiteNode(), personNode(), article,
                breadcrumb(List.of(crumb("Accueil", "/"), crumb("Paracha", "/paracha"), crumb("Paracha " + p.getName(), path)))));

        StringBuilder body = new StringBuilder("<article><h1>Paracha ").append(esc(p.getName()));
        if (p.getHebrewName() != null) body.append(" <span lang=\"he\" dir=\"rtl\">").append(esc(p.getHebrewName())).append("</span>");
        body.append("</h1><p>").append(esc(p.getBook())).append("</p>").append(markdown.toHtml(p.getIntro()));
        int i = 1;
        for (Aliyah a : p.getAliyot()) {
            String label = a.getLabel() == null || a.getLabel().isBlank() ? "Montée " + i : a.getLabel();
            body.append("<section><h2>").append(esc(label)).append("</h2>").append(markdown.toHtml(a.getContent())).append("</section>");
            i++;
        }
        body.append("</article>");
        return SeoPage.ok(compose(base, h, body.toString()));
    }


    private SeoPage feteList(String base) {
        List<FeteSummary> all = fetes.findAllByStatutOrderByPositionAscNameAsc(StatutArticle.PUBLISHED);
        Head h = new Head("Fêtes · Chemin de Guer", "Mes résumés des fêtes juives : histoire, coutumes, prières et ce que j'en ai appris.", "/fetes");
        h.ogType = "website";
        h.jsonLd.add(graph(websiteNode(), personNode(), collectionPage("Fêtes", h.description, "/fetes"),
                breadcrumb(List.of(crumb("Accueil", "/"), crumb("Fêtes", "/fetes")))));
        StringBuilder body = new StringBuilder("<h1>Fêtes</h1>");
        String category = null;
        for (FeteSummary f : all) {
            if (!f.getCategory().equals(category)) {
                if (category != null) body.append("</ul>");
                category = f.getCategory();
                body.append("<h2>").append(esc(category)).append("</h2><ul>");
            }
            body.append("<li><a href=\"").append(esc(site.url("/fetes/" + f.getSlug()))).append("\">").append(esc(f.getName())).append("</a>");
            if (f.getHebrewName() != null) body.append(" <span lang=\"he\" dir=\"rtl\">").append(esc(f.getHebrewName())).append("</span>");
            body.append("</li>");
        }
        if (category != null) body.append("</ul>");
        return SeoPage.ok(compose(base, h, body.toString()));
    }

    private SeoPage fete(String base, String slug) {
        Optional<FeteSummary> found = fetes.findBySlugAndStatut(slug, StatutArticle.PUBLISHED);
        if (found.isEmpty()) return notFound(base);
        FeteSummary f = found.get();
        String path = "/fetes/" + f.getSlug();
        String intro = Excerpts.of(f.getIntro(), 160);
        String description = intro.isBlank() ? "Mon résumé de la fête de " + f.getName() + " : histoire, coutumes et ce que j'en ai appris." : intro;
        java.time.LocalDate next = calendar.nextOccurrence(f.getSlug());
        Head h = new Head(f.getName() + " : résumé de la fête · " + site.name(), description, path);
        h.ogTitle = f.getName();
        h.ogType = "article";
        h.image = "/og/fetes/" + f.getSlug() + ".png";
        h.imageAlt = f.getName() + " · " + site.name();
        h.modified = f.getUpdatedAt();
        h.published = f.getCreatedAt();
        h.tags = List.of("Fêtes", f.getCategory());

        Map<String, Object> article = new LinkedHashMap<>();
        article.put("@type", "Article");
        article.put("@id", site.url(path) + "#article");
        article.put("headline", f.getName() + " : résumé de la fête");
        article.put("description", description);
        article.put("url", site.url(path));
        article.put("mainEntityOfPage", Map.of("@type", "WebPage", "@id", site.url(path)));
        article.put("datePublished", iso(f.getCreatedAt()));
        article.put("dateModified", iso(f.getUpdatedAt()));
        article.put("inLanguage", "fr-FR");
        article.put("about", f.getName() + (f.getHebrewName() != null ? " (" + f.getHebrewName() + ")" : ""));
        article.put("author", Map.of("@id", site.url("/#person")));
        article.put("publisher", Map.of("@id", site.url("/#person")));
        article.put("isPartOf", Map.of("@id", site.url("/#website")));
        article.put("image", List.of(site.url(h.image)));
        h.jsonLd.add(graph(websiteNode(), personNode(), article,
                breadcrumb(List.of(crumb("Accueil", "/"), crumb("Fêtes", "/fetes"), crumb(f.getName(), path)))));

        StringBuilder body = new StringBuilder("<article><h1>").append(esc(f.getName()));
        if (f.getHebrewName() != null) body.append(" <span lang=\"he\" dir=\"rtl\">").append(esc(f.getHebrewName())).append("</span>");
        body.append("</h1><p>").append(esc(f.getCategory())).append("</p>");
        if (next != null) body.append("<p>Prochaine date : <time datetime=\"").append(next).append("\">").append(FETE_DATE.format(next)).append("</time></p>");
        body.append(markdown.toHtml(f.getIntro()));
        int i = 1;
        for (FeteSection sec : f.getSections()) {
            String label = sec.getLabel() == null || sec.getLabel().isBlank() ? "Partie " + i : sec.getLabel();
            body.append("<section><h2>").append(esc(label)).append("</h2>").append(markdown.toHtml(sec.getContent())).append("</section>");
            i++;
        }
        body.append("</article>");
        return SeoPage.ok(compose(base, h, body.toString()));
    }

    private SeoPage notFound(String base) {
        Head h = new Head("Page introuvable · " + site.name(), "Cette page n'existe pas.", null);
        h.robots = "noindex, nofollow";
        String body = "<h1>Page introuvable</h1><p><a href=\"" + esc(site.url("/")) + "\">Retour à l'accueil</a></p>";
        return SeoPage.notFound(compose(base, h, body));
    }

    // ------------------------------------------------------------------ assemblage

    private static class Head {
        final String title;
        final String description;
        final String path; // null = pas de canonical
        String ogTitle;
        String ogType = "website";
        String image = OG_DEFAULT;
        String imageAlt;
        String robots = ROBOTS_INDEX;
        Instant published, modified;
        List<String> tags = List.of();
        final List<Map<String, Object>> jsonLd = new ArrayList<>();

        Head(String title, String description, String path) {
            this.title = title;
            this.description = description;
            this.path = path;
        }
    }

    private String compose(String shellHtml, Head h, String bodyHtml) {
        String ogTitle = h.ogTitle != null ? h.ogTitle : h.title;
        String imageUrl = site.url(h.image);
        String imageAlt = h.imageAlt != null ? h.imageAlt : site.name() + " · journal de conversion au judaïsme";
        StringBuilder t = new StringBuilder("\n<!-- SEO : balises générées par le serveur -->\n");
        t.append("<title>").append(esc(h.title)).append("</title>\n");
        t.append(meta("name", "description", h.description));
        t.append(meta("name", "robots", site.indexable() ? h.robots : ROBOTS_NOINDEX));
        t.append(meta("name", "author", site.authorName()));
        if (h.path != null) {
            String url = site.url("/".equals(h.path) ? "/" : h.path);
            t.append("<link rel=\"canonical\" href=\"").append(esc(url)).append("\">\n");
            t.append("<link rel=\"alternate\" hreflang=\"fr\" href=\"").append(esc(url)).append("\">\n");
            t.append("<link rel=\"alternate\" hreflang=\"x-default\" href=\"").append(esc(url)).append("\">\n");
            t.append(meta("property", "og:url", url));
        }
        t.append(meta("property", "og:site_name", site.name()));
        t.append(meta("property", "og:locale", "fr_FR"));
        t.append(meta("property", "og:type", h.ogType));
        t.append(meta("property", "og:title", ogTitle));
        t.append(meta("property", "og:description", h.description));
        t.append(meta("property", "og:image", imageUrl));
        t.append(meta("property", "og:image:type", "image/png"));
        t.append(meta("property", "og:image:width", "1200"));
        t.append(meta("property", "og:image:height", "630"));
        t.append(meta("property", "og:image:alt", imageAlt));
        if ("article".equals(h.ogType)) {
            if (h.published != null) t.append(meta("property", "article:published_time", iso(h.published)));
            if (h.modified != null) t.append(meta("property", "article:modified_time", iso(h.modified)));
            t.append(meta("property", "article:author", site.authorUrl()));
            for (String tag : h.tags) t.append(meta("property", "article:tag", tag));
        }
        t.append(meta("name", "twitter:card", "summary_large_image"));
        t.append(meta("name", "twitter:title", ogTitle));
        t.append(meta("name", "twitter:description", h.description));
        t.append(meta("name", "twitter:image", imageUrl));
        t.append(meta("name", "twitter:image:alt", imageAlt));
        for (Map<String, Object> ld : h.jsonLd) {
            t.append("<script type=\"application/ld+json\">").append(Html.jsonForScript(json.writeValueAsString(ld))).append("</script>\n");
        }

        String out = shellHtml;
        // Retire les balises génériques de la page de base : elles sont remplacées par celles de la page demandée
        out = out.replaceAll("(?is)<title>.*?</title>", "");
        out = out.replaceAll("(?i)<meta\\s+(?:name|property)=\"(?:description|robots|author|og:[^\"]*|twitter:[^\"]*)\"[^>]*>", "");
        out = out.replaceAll("(?i)<link\\s+rel=\"canonical\"[^>]*>", "");
        out = out.replace("</head>", t + "</head>");
        // Texte de la page, masqué visuellement : lisible par les robots, remplacé au démarrage de l'application
        String prerender = "<div id=\"seo-prerender\" aria-hidden=\"true\">" + bodyHtml + "</div>";
        out = out.replaceFirst("(?i)<app-root(\\s[^>]*)?>\\s*</app-root>", java.util.regex.Matcher.quoteReplacement("<app-root>" + prerender + "</app-root>"));
        return out;
    }

    private static String meta(String attr, String name, String content) {
        return "<meta " + attr + "=\"" + esc(name) + "\" content=\"" + esc(content) + "\">\n";
    }

    // ------------------------------------------------------------------ données structurées

    private Map<String, Object> graph(Map<String, Object>... nodes) {
        Map<String, Object> g = new LinkedHashMap<>();
        g.put("@context", "https://schema.org");
        g.put("@graph", List.of(nodes));
        return g;
    }

    private Map<String, Object> websiteNode() {
        Map<String, Object> w = new LinkedHashMap<>();
        w.put("@type", "WebSite");
        w.put("@id", site.url("/#website"));
        w.put("url", site.url("/"));
        w.put("name", site.name());
        w.put("description", site.description());
        w.put("inLanguage", "fr-FR");
        w.put("publisher", Map.of("@id", site.url("/#person")));
        w.put("potentialAction", Map.of("@type", "SearchAction",
                "target", Map.of("@type", "EntryPoint", "urlTemplate", site.url("/articles?q={search_term_string}")),
                "query-input", "required name=search_term_string"));
        return w;
    }

    private Map<String, Object> personNode() {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("@type", "Person");
        p.put("@id", site.url("/#person"));
        p.put("name", site.authorName());
        if (!site.authorUrl().isBlank()) {
            p.put("url", site.authorUrl());
            p.put("sameAs", List.of(site.authorUrl()));
        }
        return p;
    }

    private Map<String, Object> blogNode() {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("@type", "Blog");
        b.put("@id", site.url("/#blog"));
        b.put("url", site.url("/"));
        b.put("name", site.name());
        b.put("description", site.description());
        b.put("inLanguage", "fr-FR");
        b.put("author", Map.of("@id", site.url("/#person")));
        return b;
    }

    private Map<String, Object> collectionPage(String name, String description, String path) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("@type", "CollectionPage");
        c.put("@id", site.url(path) + "#page");
        c.put("url", site.url(path));
        c.put("name", name);
        c.put("description", description);
        c.put("inLanguage", "fr-FR");
        c.put("isPartOf", Map.of("@id", site.url("/#website")));
        return c;
    }

    private Map<String, Object> crumb(String name, String path) {
        return Map.of("name", name, "item", site.url(path));
    }

    private Map<String, Object> breadcrumb(List<Map<String, Object>> crumbs) {
        List<Map<String, Object>> items = new ArrayList<>();
        int pos = 1;
        for (Map<String, Object> c : crumbs) {
            items.add(Map.of("@type", "ListItem", "position", pos++, "name", c.get("name"), "item", c.get("item")));
        }
        return Map.of("@type", "BreadcrumbList", "itemListElement", items);
    }

    // ------------------------------------------------------------------ utilitaires

    private String articleLinks(List<Article> list) {
        if (list.isEmpty()) return "<p>Aucun article pour le moment.</p>";
        StringBuilder sb = new StringBuilder("<ul>");
        for (Article a : list) {
            String path = "/articles/" + (a.getSlug() != null ? a.getSlug() : a.getId());
            sb.append("<li><a href=\"").append(esc(site.url(path))).append("\">").append(esc(a.getTitle())).append("</a> : ")
                    .append(esc(description(a))).append("</li>");
        }
        return sb.append("</ul>").toString();
    }

    static String normalize(String rawPath) {
        String p = rawPath == null || rawPath.isBlank() ? "/" : rawPath.split("[?#]")[0];
        if (!p.startsWith("/")) p = "/" + p;
        return p.length() > 1 ? p.replaceAll("/+$", "") : p;
    }

    /** Description rédigée par l'auteur, sinon premier vrai paragraphe du texte (160 caractères au plus). */
    static String description(Article a) {
        if (a.getDescription() != null && !a.getDescription().isBlank()) return a.getDescription().trim();
        return Excerpts.of(a.getContent(), 160);
    }

    static String excerpt(String content) {
        return Excerpts.of(content, 160);
    }

    private static int wordCount(String content) {
        return content == null || content.isBlank() ? 0 : content.trim().split("\\s+").length;
    }

    private static String iso(Instant i) {
        return i == null ? null : i.toString();
    }
}
