package com.chemindeguer.backend.calendar;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record CalendarResponse(String hebrewDate, String hebrewDateHe, ParashaInfo parasha, Instant candleLighting,
                               Instant havdalah, String city, List<Holiday> holidays) {

    public record ParashaInfo(String name, String slug, String hebrew, String torah, String haftarah, String summarySlug) {
    }

    public record Holiday(LocalDate date, String title, String hebrew) {
    }
}
