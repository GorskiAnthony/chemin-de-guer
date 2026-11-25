package com.chemindeguer.backend.stats;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class StatsService {
    /** Durée de conservation : 13 mois, de quoi comparer d'une année sur l'autre sans garder indéfiniment. */
    static final int RETENTION_DAYS = 400;
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    public record Day(LocalDate day, long views, long visitors) {}
    public record Row(String label, long views, long visitors) {}
    public record Summary(int days, long views, long visits, List<Day> perDay, List<Row> pages, List<Row> referrers, List<Row> devices) {}

    private final PageViewRepository repository;
    private final VisitorHasher hasher;
    private final String ownHost;

    public StatsService(PageViewRepository repository, VisitorHasher hasher, @Value("${app.public-url}") String publicUrl) {
        this.repository = repository;
        this.hasher = hasher;
        String host = null;
        try {
            host = URI.create(publicUrl).getHost();
        } catch (IllegalArgumentException ignored) {
            // adresse publique absente ou invalide : seul le tri des liens internes est moins précis
        }
        this.ownHost = host;
    }

    @Transactional
    public void record(String rawPath, String referrer, String userAgent, String ip) {
        String path = HitRules.normalizePath(rawPath);
        if (path == null || HitRules.isBot(userAgent)) return;
        LocalDate day = LocalDate.now(PARIS);
        var view = new PageView();
        view.setDay(day);
        view.setViewedAt(Instant.now());
        view.setPath(path);
        view.setReferrerHost(HitRules.referrerHost(referrer, ownHost));
        view.setDevice(HitRules.device(userAgent));
        view.setVisitor(hasher.hash(day, ip, userAgent));
        repository.save(view);
    }

    @Transactional(readOnly = true)
    public Summary summary(int days) {
        int span = Math.max(1, Math.min(days, RETENTION_DAYS));
        LocalDate today = LocalDate.now(PARIS);
        LocalDate from = today.minusDays(span - 1L);

        // Une ligne par jour, y compris les jours sans visite (pour un graphique continu)
        var byDay = new java.util.HashMap<LocalDate, Day>();
        for (Object[] r : repository.perDay(from)) {
            byDay.put((LocalDate) r[0], new Day((LocalDate) r[0], (Long) r[1], (Long) r[2]));
        }
        List<Day> perDay = new ArrayList<>();
        long views = 0, visits = 0;
        for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) {
            Day row = byDay.getOrDefault(d, new Day(d, 0, 0));
            perDay.add(row);
            views += row.views();
            visits += row.visitors();
        }
        var top = PageRequest.of(0, 10);
        return new Summary(span, views, visits, perDay,
                repository.topPages(from, top).stream().map(r -> new Row((String) r[0], (Long) r[1], (Long) r[2])).toList(),
                repository.topReferrers(from, top).stream().map(r -> new Row((String) r[0], (Long) r[1], 0)).toList(),
                repository.devices(from).stream().map(r -> new Row((String) r[0], (Long) r[1], 0)).toList());
    }

    @Scheduled(cron = "0 15 4 * * *", zone = "Europe/Paris")
    @Transactional
    public void purge() {
        repository.deleteOlderThan(LocalDate.now(PARIS).minusDays(RETENTION_DAYS));
    }
}
