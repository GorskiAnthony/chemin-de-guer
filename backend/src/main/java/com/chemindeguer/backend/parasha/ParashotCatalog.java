package com.chemindeguer.backend.parasha;

import com.chemindeguer.backend.common.Slugs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Les 54 parashiot dans l'ordre de la Torah, avec l'orthographe utilisée par Hebcal. */
public final class ParashotCatalog {
    public record Entry(String name, String book, int position) {
    }

    private static final Map<String, List<String>> BOOKS = new java.util.LinkedHashMap<>();

    static {
        BOOKS.put("Berechit", List.of("Bereshit", "Noach", "Lech-Lecha", "Vayera", "Chayei Sara", "Toldot", "Vayetzei",
                "Vayishlach", "Vayeshev", "Miketz", "Vayigash", "Vayechi"));
        BOOKS.put("Chemot", List.of("Shemot", "Vaera", "Bo", "Beshalach", "Yitro", "Mishpatim", "Terumah", "Tetzaveh",
                "Ki Tisa", "Vayakhel", "Pekudei"));
        BOOKS.put("Vayikra", List.of("Vayikra", "Tzav", "Shmini", "Tazria", "Metzora", "Achrei Mot", "Kedoshim", "Emor",
                "Behar", "Bechukotai"));
        BOOKS.put("Bamidbar", List.of("Bamidbar", "Nasso", "Beha'alotcha", "Sh'lach", "Korach", "Chukat", "Balak",
                "Pinchas", "Matot", "Masei"));
        BOOKS.put("Devarim", List.of("Devarim", "Vaetchanan", "Eikev", "Re'eh", "Shoftim", "Ki Teitzei", "Ki Tavo",
                "Nitzavim", "Vayeilech", "Ha'azinu", "Vezot Haberakhah"));
    }

    public static final List<Entry> ENTRIES;
    private static final Map<String, Entry> BY_SLUG;

    static {
        List<Entry> list = new ArrayList<>();
        int pos = 0;
        for (var e : BOOKS.entrySet()) {
            for (String name : e.getValue()) {
                list.add(new Entry(name, e.getKey(), pos++));
            }
        }
        ENTRIES = List.copyOf(list);
        BY_SLUG = ENTRIES.stream().collect(Collectors.toMap(e -> Slugs.of(e.name()), e -> e, (a, b) -> a));
    }

    private ParashotCatalog() {
    }

    public static Entry find(String name) {
        return BY_SLUG.get(Slugs.of(name));
    }
}
