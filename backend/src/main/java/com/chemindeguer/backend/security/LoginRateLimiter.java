package com.chemindeguer.backend.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/** Limite simple en mémoire : 5 échecs de connexion par IP et par fenêtre de 15 minutes. */
@Component
public class LoginRateLimiter {
    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private record Entry(int failures, Instant windowStart) {
    }

    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        Entry e = entries.get(key);
        if (e == null) return false;
        if (Instant.now().isAfter(e.windowStart().plus(WINDOW))) {
            entries.remove(key);
            return false;
        }
        return e.failures() >= MAX_FAILURES;
    }

    public void recordFailure(String key) {
        Instant now = Instant.now();
        entries.merge(key, new Entry(1, now), (old, ignored) ->
                now.isAfter(old.windowStart().plus(WINDOW)) ? new Entry(1, now) : new Entry(old.failures() + 1, old.windowStart()));
    }

    public void reset(String key) {
        entries.remove(key);
    }
}
