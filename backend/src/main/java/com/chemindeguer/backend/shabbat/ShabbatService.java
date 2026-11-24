package com.chemindeguer.backend.shabbat;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.*;
import java.util.List;
import java.util.Map;

@Service
public class ShabbatService {
    private static final Logger log = LoggerFactory.getLogger(ShabbatService.class);
    private static final Duration CACHE_TTL = Duration.ofHours(6);

    private final ShabbatProperties props;
    private final RestClient restClient;
    private final Clock clock;

    private volatile Cached cached;

    private record Cached(LocalDate friday, Instant fetchedAt, ShabbatWindow window, String parasha,
                           String parashaHebrew, String city) {
    }

    @Autowired
    public ShabbatService(ShabbatProperties props) {
        this(props, RestClient.builder(), Clock.systemUTC());
    }

    ShabbatService(ShabbatProperties props, RestClient.Builder builder, Clock clock) {
        this.props = props;
        this.restClient = builder.build();
        this.clock = clock;
    }

    /** En cas d'échec de l'API, le site reste ouvert (échec ouvert) mais l'erreur est journalisée. */
    public ShabbatStatus status() {
        if (!props.enabled()) {
            return new ShabbatStatus(false, null, null, null, null, null);
        }
        Instant now = clock.instant();
        Cached data = load(now);
        if (data == null) {
            return new ShabbatStatus(false, null, null, null, null, null);
        }
        return new ShabbatStatus(data.window().contains(now), data.window().start(), data.window().end(),
                data.parasha(), data.parashaHebrew(), data.city());
    }

    public boolean isClosed() {
        return status().closed();
    }

    @SuppressWarnings("unchecked")
    private synchronized Cached load(Instant now) {
        LocalDate friday = ShabbatWindow.referenceFriday(LocalDate.ofInstant(now, ZoneId.of(props.timezone())));
        Cached c = cached;
        if (c != null && c.friday().equals(friday) && Duration.between(c.fetchedAt(), now).compareTo(CACHE_TTL) < 0) {
            return c;
        }
        try {
            Map<String, Object> body = restClient.get()
                    .uri(props.apiUrl() + "?cfg=json&M=on&geonameid={g}&gy={y}&gm={m}&gd={d}",
                            props.geonameId(), friday.getYear(), friday.getMonthValue(), friday.getDayOfMonth())
                    .retrieve().body(Map.class);
            c = parse(body, friday, now);
            cached = c;
            return c;
        } catch (Exception e) {
            log.warn("Hebcal indisponible : {}", e.getMessage());
            return (c != null && c.friday().equals(friday)) ? c : null;
        }
    }

    @SuppressWarnings("unchecked")
    private Cached parse(Map<String, Object> body, LocalDate friday, Instant now) {
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        Instant candles = null, havdalah = null;
        String parasha = null, hebrew = null;
        for (Map<String, Object> item : items) {
            String cat = String.valueOf(item.get("category"));
            switch (cat) {
                case "candles" -> candles = OffsetDateTime.parse((String) item.get("date")).toInstant();
                case "havdalah" -> havdalah = OffsetDateTime.parse((String) item.get("date")).toInstant();
                case "parashat" -> {
                    parasha = (String) item.get("title");
                    hebrew = (String) item.get("hebrew");
                }
                default -> {
                }
            }
        }
        if (candles == null || havdalah == null) {
            throw new IllegalStateException("Horaires incomplets dans la réponse Hebcal");
        }
        String city = body.get("location") instanceof Map<?, ?> loc ? String.valueOf(loc.get("city")) : null;
        ShabbatWindow window = new ShabbatWindow(candles.minus(Duration.ofMinutes(props.marginMinutes())), havdalah);
        return new Cached(friday, now, window, parasha, hebrew, city);
    }
}
