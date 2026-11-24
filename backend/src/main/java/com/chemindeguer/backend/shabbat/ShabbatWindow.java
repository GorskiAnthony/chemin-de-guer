package com.chemindeguer.backend.shabbat;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;

/** Fenêtre de fermeture : de (entrée du Shabbat - marge) jusqu'à la sortie. */
public record ShabbatWindow(Instant start, Instant end) {

    public boolean contains(Instant now) {
        return !now.isBefore(start) && now.isBefore(end);
    }

    /** Vendredi du week-end concerné : le vendredi du jour, ou le prochain, ou la veille si samedi. */
    public static LocalDate referenceFriday(LocalDate today) {
        return switch (today.getDayOfWeek()) {
            case SATURDAY -> today.minusDays(1);
            case FRIDAY -> today;
            default -> today.with(java.time.temporal.TemporalAdjusters.next(DayOfWeek.FRIDAY));
        };
    }
}
