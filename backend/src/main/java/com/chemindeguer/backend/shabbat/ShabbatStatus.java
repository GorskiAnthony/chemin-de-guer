package com.chemindeguer.backend.shabbat;

import java.time.Instant;

public record ShabbatStatus(boolean closed, Instant closesAt, Instant opensAt, String parasha, String parashaHebrew,
                            String city) {
}
