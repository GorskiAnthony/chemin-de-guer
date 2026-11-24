package com.chemindeguer.backend.comment;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class CommentRateLimiterTest {
    private final CommentRateLimiter limiter = new CommentRateLimiter();
    private final Instant t0 = Instant.parse("2026-11-24T10:00:00Z");

    @Test
    void refuseApresLaLimite() {
        for (int i = 0; i < CommentRateLimiter.MAX_PER_WINDOW; i++) {
            assertTrue(limiter.tryAcquire("1.2.3.4", t0.plusSeconds(i)));
        }
        assertFalse(limiter.tryAcquire("1.2.3.4", t0.plusSeconds(10)));
    }

    @Test
    void chaqueVisiteurALaSienneLimite() {
        for (int i = 0; i < CommentRateLimiter.MAX_PER_WINDOW; i++) {
            limiter.tryAcquire("1.2.3.4", t0);
        }
        assertFalse(limiter.tryAcquire("1.2.3.4", t0));
        assertTrue(limiter.tryAcquire("5.6.7.8", t0));
    }

    @Test
    void lAutorisationRevientApresLaFenetre() {
        for (int i = 0; i < CommentRateLimiter.MAX_PER_WINDOW; i++) {
            limiter.tryAcquire("1.2.3.4", t0);
        }
        assertFalse(limiter.tryAcquire("1.2.3.4", t0.plusSeconds(60)));
        assertTrue(limiter.tryAcquire("1.2.3.4", t0.plus(CommentRateLimiter.WINDOW).plusSeconds(1)));
    }
}
