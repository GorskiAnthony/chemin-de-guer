package com.chemindeguer.backend.calendar;

import com.chemindeguer.backend.article.StatutArticle;
import com.chemindeguer.backend.calendar.CalendarResponse.Holiday;
import com.chemindeguer.backend.calendar.CalendarResponse.ParashaInfo;
import com.chemindeguer.backend.common.Slugs;
import com.chemindeguer.backend.parasha.ParashaRepository;
import com.chemindeguer.backend.shabbat.ShabbatProperties;
import com.chemindeguer.backend.shabbat.ShabbatWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.text.Normalizer;
import java.time.*;
import java.util.*;

/** Calendrier hébraïque (date, paracha, fêtes) via Hebcal, mis en cache pour la journée. */
@Service
public class CalendarService {
    private static final Logger log = LoggerFactory.getLogger(CalendarService.class);
    private static final Duration TTL = Duration.ofHours(6);
    private static final int MAX_HOLIDAYS = 5;

    private final ShabbatProperties props;
    private final ParashaRepository parashaRepository;
    private final RestClient client = RestClient.create();
    private final Clock clock = Clock.systemUTC();
    private final String origin;

    private volatile Cached cached;

    private record Cached(LocalDate day, Instant fetchedAt, CalendarResponse data) {
    }

    public CalendarService(ShabbatProperties props, ParashaRepository parashaRepository) {
        this.props = props;
        this.parashaRepository = parashaRepository;
        this.origin = props.apiUrl().replaceAll("^(https?://[^/]+).*$", "$1");
    }

    public CalendarResponse get() {
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, ZoneId.of(props.timezone()));
        Cached c = cached;
        if (c == null || !c.day().equals(today) || Duration.between(c.fetchedAt(), now).compareTo(TTL) > 0) {
            try {
                c = new Cached(today, now, fetch(today));
                cached = c;
            } catch (Exception e) {
                log.warn("Calendrier hébraïque indisponible : {}", e.getMessage());
                if (c == null) {
                    return new CalendarResponse(null, null, null, null, null, null, List.of());
                }
            }
        }
        return withSummary(c.data());
    }

    /** Le lien vers le résumé est évalué à chaque appel : un résumé publié apparaît immédiatement. */
    private CalendarResponse withSummary(CalendarResponse d) {
        ParashaInfo p = d.parasha();
        if (p == null) return d;
        String summary = parashaRepository.existsBySlugAndStatut(p.slug(), StatutArticle.PUBLISHED) ? p.slug() : null;
        return new CalendarResponse(d.hebrewDate(), d.hebrewDateHe(),
                new ParashaInfo(p.name(), p.slug(), p.hebrew(), p.torah(), p.haftarah(), summary),
                d.candleLighting(), d.havdalah(), d.city(), d.holidays());
    }

    @SuppressWarnings("unchecked")
    private CalendarResponse fetch(LocalDate today) {
        Map<String, Object> conv = client.get()
                .uri(origin + "/converter?cfg=json&g2h=1&gy={y}&gm={m}&gd={d}", today.getYear(), today.getMonthValue(), today.getDayOfMonth())
                .retrieve().body(Map.class);
        String hebrewDate = conv.get("hd") + " " + conv.get("hm") + " " + conv.get("hy");
        String hebrewDateHe = (String) conv.get("hebrew");

        LocalDate friday = ShabbatWindow.referenceFriday(today);
        Map<String, Object> shabbat = client.get()
                .uri(origin + "/shabbat?cfg=json&M=on&geonameid={g}&gy={y}&gm={m}&gd={d}",
                        props.geonameId(), friday.getYear(), friday.getMonthValue(), friday.getDayOfMonth())
                .retrieve().body(Map.class);
        ParashaInfo parasha = null;
        Instant candles = null, havdalah = null;
        for (Map<String, Object> item : (List<Map<String, Object>>) shabbat.get("items")) {
            switch (String.valueOf(item.get("category"))) {
                case "candles" -> candles = OffsetDateTime.parse((String) item.get("date")).toInstant();
                case "havdalah" -> havdalah = OffsetDateTime.parse((String) item.get("date")).toInstant();
                case "parashat" -> {
                    String name = ((String) item.get("title")).replaceFirst("^Parashat\\s+", "");
                    Map<String, Object> ley = item.get("leyning") instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
                    parasha = new ParashaInfo(name, Slugs.of(name), (String) item.get("hebrew"),
                            (String) ley.get("torah"), (String) ley.get("haftarah"), null);
                }
                default -> {
                }
            }
        }
        String city = shabbat.get("location") instanceof Map<?, ?> loc ? String.valueOf(loc.get("city")) : null;
        return new CalendarResponse(hebrewDate, hebrewDateHe, parasha, candles, havdalah, city, holidays(today));
    }

    @SuppressWarnings("unchecked")
    private List<Holiday> holidays(LocalDate today) {
        Map<String, Object> body = client.get()
                .uri(origin + "/hebcal?v=1&cfg=json&maj=on&min=on&nx=on&mf=on&mod=off&ss=off&i=off&lg=fr&start={s}&end={e}",
                        today, today.plusDays(150))
                .retrieve().body(Map.class);
        List<Holiday> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Map<String, Object> item : (List<Map<String, Object>>) body.get("items")) {
            String category = String.valueOf(item.get("category"));
            if (!category.equals("holiday") && !category.equals("roshchodesh")) continue;
            String title = clean((String) item.get("title"));
            String hebrew = (String) item.get("hebrew");
            if (title.contains("Banot")) continue;
            // "Hanoukah: 3 Bougies" -> une seule entrée "Hanoukah" (premier jour)
            if (title.contains(":")) {
                title = title.substring(0, title.indexOf(':'));
                if (hebrew != null && hebrew.contains(":")) hebrew = hebrew.substring(0, hebrew.indexOf(':'));
            }
            if (!seen.add(title)) continue;
            out.add(new Holiday(LocalDate.parse(((String) item.get("date")).substring(0, 10)), title, hebrew));
            if (out.size() == MAX_HOLIDAYS) break;
        }
        return out;
    }

    /** Retire les signes diacritiques combinants que Hebcal ajoute en français (H̲anoukah). */
    private static String clean(String s) {
        return Normalizer.normalize(Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", ""), Normalizer.Form.NFC);
    }
}
