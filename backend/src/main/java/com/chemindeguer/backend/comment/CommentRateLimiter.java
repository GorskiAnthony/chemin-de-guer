package com.chemindeguer.backend.comment;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;

/** Anti-inondation : au plus 3 commentaires par visiteur (IP) et par fenêtre de 10 minutes. */
@Component
public class CommentRateLimiter {
    static final int MAX_PER_WINDOW = 3;
    static final Duration WINDOW = Duration.ofMinutes(10);
    private static final int CLEANUP_THRESHOLD = 5_000;

    private final ConcurrentHashMap<String, ArrayDeque<Instant>> events = new ConcurrentHashMap<>();

    /** Enregistre une tentative et renvoie false si la limite est dépassée. */
    public boolean tryAcquire(String key) {
        return tryAcquire(key, Instant.now());
    }

    boolean tryAcquire(String key, Instant now) {
        if (events.size() > CLEANUP_THRESHOLD) {
            events.entrySet().removeIf(e -> expired(e.getValue(), now));
        }
        ArrayDeque<Instant> q = events.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && q.peekFirst().isBefore(now.minus(WINDOW))) {
                q.pollFirst();
            }
            if (q.size() >= MAX_PER_WINDOW) {
                return false;
            }
            q.addLast(now);
            return true;
        }
    }

    private static boolean expired(ArrayDeque<Instant> q, Instant now) {
        synchronized (q) {
            return q.isEmpty() || q.peekLast().isBefore(now.minus(WINDOW));
        }
    }
}
