package com.chemindeguer.backend.seo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;
import java.util.List;

/**
 * Génère l'image de partage (1200x630) d'un article ou d'une paracha : le titre sur le fond du site.
 * Les images sont mises en cache en mémoire. En cas d'indisponibilité du rendu graphique, renvoie null
 * et l'appelant retombe sur l'image par défaut.
 */
@Service
public class OgImageService {
    private static final Logger log = LoggerFactory.getLogger(OgImageService.class);
    private static final int W = 1200, H = 630, LEFT = 80, TEXT_WIDTH = 860;
    private static final Color BG = new Color(11, 31, 60), BG2 = new Color(21, 51, 92), GOLD = new Color(227, 187, 92),
            CREAM = new Color(236, 230, 214);
    private static final int CACHE_MAX = 200;

    private final SiteProperties site;
    private final Font regular;
    private final Font bold;
    private final Map<String, byte[]> cache = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
            return size() > CACHE_MAX;
        }
    });

    public OgImageService(SiteProperties site) {
        this.site = site;
        System.setProperty("java.awt.headless", "true");
        Font r = null, b = null;
        try {
            r = load("/fonts/FrankRuhlLibre-Regular.ttf");
            b = load("/fonts/FrankRuhlLibre-Bold.ttf");
        } catch (Throwable e) {
            log.warn("Polices des images de partage indisponibles : {}", e.toString());
        }
        this.regular = r;
        this.bold = b;
    }

    private static Font load(String resource) throws Exception {
        try (InputStream in = OgImageService.class.getResourceAsStream(resource)) {
            return Font.createFont(Font.TRUETYPE_FONT, in);
        }
    }

    /** @return le PNG, ou null si le rendu est impossible */
    public byte[] render(String cacheKey, String kicker, String title, String hebrew) {
        if (regular == null) return null;
        byte[] hit = cache.get(cacheKey);
        if (hit != null) return hit;
        try {
            byte[] png = draw(kicker, title, hebrew);
            cache.put(cacheKey, png);
            return png;
        } catch (Throwable e) {
            log.warn("Génération de l'image de partage impossible : {}", e.toString());
            return null;
        }
    }

    private byte[] draw(String kicker, String title, String hebrew) throws Exception {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            g.setPaint(new GradientPaint(0, 0, BG, 0, H, BG2));
            g.fillRect(0, 0, W, H);
            // motif diagonal discret
            g.setColor(new Color(255, 255, 255, 9));
            g.setStroke(new BasicStroke(10));
            for (int x = -H; x < W; x += 28) g.drawLine(x, H, x + H, 0);
            // lettre ג en filigrane
            g.setFont(bold.deriveFont(620f));
            g.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 70));
            FontMetrics gm = g.getFontMetrics();
            String gimel = "ג";
            g.drawString(gimel, W - 70 - gm.stringWidth(gimel), H / 2 + (gm.getAscent() - gm.getDescent()) / 2 - 20);
            // bandeau doré en haut
            g.setPaint(new GradientPaint(0, 0, BG2, W, 0, GOLD));
            g.fillRect(0, 0, W, 11);

            // surtitre
            Font kickerFont = regular.deriveFont(Map.of(TextAttribute.SIZE, 28f, TextAttribute.TRACKING, 0.12f));
            g.setFont(kickerFont);
            g.setColor(GOLD);
            g.drawString(kicker.toUpperCase(Locale.FRENCH), LEFT, 120);

            // titre : taille adaptée à la longueur, 4 lignes maximum
            int size = 92;
            List<String> lines;
            FontMetrics fm;
            while (true) {
                g.setFont(bold.deriveFont((float) size));
                fm = g.getFontMetrics();
                lines = wrap(title, fm, TEXT_WIDTH);
                if (lines.size() <= (size > 70 ? 3 : 4) || size <= 48) break;
                size -= 6;
            }
            if (lines.size() > 4) {
                lines = new ArrayList<>(lines.subList(0, 4));
                String last = lines.get(3);
                while (fm.stringWidth(last + "…") > TEXT_WIDTH && last.contains(" ")) last = last.substring(0, last.lastIndexOf(' '));
                lines.set(3, last + "…");
            }
            int lineHeight = (int) (size * 1.12);
            int y = 215 + (fm.getAscent() - 20);
            g.setColor(Color.WHITE);
            for (String line : lines) {
                g.drawString(line, LEFT, y);
                y += lineHeight;
            }
            if (hebrew != null && !hebrew.isBlank()) {
                g.setFont(regular.deriveFont(60f));
                g.setColor(GOLD);
                g.drawString(hebrew, LEFT, y + 10);
            }

            // pied : filet doré, nom du site et auteur
            g.setColor(GOLD);
            g.fillRect(LEFT, 520, 80, 4);
            g.setFont(regular.deriveFont(30f));
            g.drawString(site.baseUrl().replaceFirst("^https?://", ""), LEFT, 568);
            g.setColor(CREAM);
            String author = site.authorName();
            g.drawString(author, W - 70 - g.getFontMetrics().stringWidth(author), 568);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    static List<String> wrap(String text, FontMetrics fm, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            String candidate = cur.length() == 0 ? word : cur + " " + word;
            if (fm.stringWidth(candidate) <= maxWidth || cur.length() == 0) {
                cur = new StringBuilder(candidate);
            } else {
                lines.add(cur.toString());
                cur = new StringBuilder(word);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }
}
