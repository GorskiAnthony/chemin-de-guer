package com.chemindeguer.backend.seo;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Heading;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Rendu Markdown côté serveur, pour le contenu pré-rendu destiné aux robots.
 * Le HTML brut saisi dans le texte est échappé, et les titres sont décalés d'un niveau
 * (le titre de l'article est le h1), comme dans le rendu du navigateur.
 */
@Component
public class MarkdownRenderer {
    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownRenderer() {
        List<Extension> extensions = List.of(TablesExtension.create(), StrikethroughExtension.create());
        this.parser = Parser.builder().extensions(extensions).build();
        this.renderer = HtmlRenderer.builder().extensions(extensions).escapeHtml(true).sanitizeUrls(true).softbreak("<br>\n").build();
    }

    public String toHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) return "";
        Node document = parser.parse(markdown);
        document.accept(new AbstractVisitor() {
            @Override
            public void visit(Heading heading) {
                heading.setLevel(Math.min(heading.getLevel() + 1, 6));
                visitChildren(heading);
            }
        });
        return renderer.render(document);
    }
}
