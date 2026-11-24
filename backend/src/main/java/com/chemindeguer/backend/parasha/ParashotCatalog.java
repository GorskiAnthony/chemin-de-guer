package com.chemindeguer.backend.parasha;

import com.chemindeguer.backend.common.Slugs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Les 54 parashiot dans l'ordre de la Torah et les 7 paracha doubles, avec l'orthographe et le nom hébreu utilisés par Hebcal
 * (les noms hébreux sont ceux de l'accueil, sans le mot « פרשת »).
 */
public final class ParashotCatalog {
    public record Entry(String name, String hebrew, String book, int position) {
    }

    public static final List<Entry> ENTRIES;
    private static final Map<String, Entry> BY_SLUG;

    static {
        List<Entry> list = new ArrayList<>();
        int[] pos = {0};
        add(list, pos, "Bereshit", "בראשית", "Berechit");
        add(list, pos, "Noach", "נח", "Berechit");
        add(list, pos, "Lech-Lecha", "לך־לך", "Berechit");
        add(list, pos, "Vayera", "וירא", "Berechit");
        add(list, pos, "Chayei Sara", "חיי שרה", "Berechit");
        add(list, pos, "Toldot", "תולדות", "Berechit");
        add(list, pos, "Vayetzei", "ויצא", "Berechit");
        add(list, pos, "Vayishlach", "וישלח", "Berechit");
        add(list, pos, "Vayeshev", "וישב", "Berechit");
        add(list, pos, "Miketz", "מקץ", "Berechit");
        add(list, pos, "Vayigash", "ויגש", "Berechit");
        add(list, pos, "Vayechi", "ויחי", "Berechit");
        add(list, pos, "Shemot", "שמות", "Chemot");
        add(list, pos, "Vaera", "וארא", "Chemot");
        add(list, pos, "Bo", "בא", "Chemot");
        add(list, pos, "Beshalach", "בשלח", "Chemot");
        add(list, pos, "Yitro", "יתרו", "Chemot");
        add(list, pos, "Mishpatim", "משפטים", "Chemot");
        add(list, pos, "Terumah", "תרומה", "Chemot");
        add(list, pos, "Tetzaveh", "תצוה", "Chemot");
        add(list, pos, "Ki Tisa", "כי תשא", "Chemot");
        add(list, pos, "Vayakhel", "ויקהל", "Chemot");
        add(list, pos, "Pekudei", "פקודי", "Chemot");
        add(list, pos, "Vayikra", "ויקרא", "Vayikra");
        add(list, pos, "Tzav", "צו", "Vayikra");
        add(list, pos, "Shmini", "שמיני", "Vayikra");
        add(list, pos, "Tazria", "תזריע", "Vayikra");
        add(list, pos, "Metzora", "מצרע", "Vayikra");
        add(list, pos, "Achrei Mot", "אחרי מות", "Vayikra");
        add(list, pos, "Kedoshim", "קדשים", "Vayikra");
        add(list, pos, "Emor", "אמור", "Vayikra");
        add(list, pos, "Behar", "בהר", "Vayikra");
        add(list, pos, "Bechukotai", "בחקתי", "Vayikra");
        add(list, pos, "Bamidbar", "במדבר", "Bamidbar");
        add(list, pos, "Nasso", "נשא", "Bamidbar");
        add(list, pos, "Beha’alotcha", "בהעלתך", "Bamidbar");
        add(list, pos, "Sh’lach", "שלח־לך", "Bamidbar");
        add(list, pos, "Korach", "קורח", "Bamidbar");
        add(list, pos, "Chukat", "חוקת", "Bamidbar");
        add(list, pos, "Balak", "בלק", "Bamidbar");
        add(list, pos, "Pinchas", "פינחס", "Bamidbar");
        add(list, pos, "Matot", "מטות", "Bamidbar");
        add(list, pos, "Masei", "מסעי", "Bamidbar");
        add(list, pos, "Devarim", "דברים", "Devarim");
        add(list, pos, "Vaetchanan", "ואתחנן", "Devarim");
        add(list, pos, "Eikev", "עקב", "Devarim");
        add(list, pos, "Re’eh", "ראה", "Devarim");
        add(list, pos, "Shoftim", "שופטים", "Devarim");
        add(list, pos, "Ki Teitzei", "כי־תצא", "Devarim");
        add(list, pos, "Ki Tavo", "כי־תבוא", "Devarim");
        add(list, pos, "Nitzavim", "נצבים", "Devarim");
        add(list, pos, "Vayeilech", "וילך", "Devarim");
        add(list, pos, "Ha’azinu", "האזינו", "Devarim");
        add(list, pos, "Vezot Haberakhah", "וזאת הברכה", "Devarim");
        // Paracha doubles (semaines où deux lectures sont réunies), en fin de catalogue pour ne pas déplacer les autres
        add(list, pos, "Vayakhel-Pekudei", "ויקהל־פקודי", "Chemot");
        add(list, pos, "Tazria-Metzora", "תזריע־מצרע", "Vayikra");
        add(list, pos, "Achrei Mot-Kedoshim", "אחרי מות־קדשים", "Vayikra");
        add(list, pos, "Behar-Bechukotai", "בהר־בחקתי", "Vayikra");
        add(list, pos, "Chukat-Balak", "חוקת־בלק", "Bamidbar");
        add(list, pos, "Matot-Masei", "מטות־מסעי", "Bamidbar");
        add(list, pos, "Nitzavim-Vayeilech", "נצבים־וילך", "Devarim");
        ENTRIES = List.copyOf(list);
        BY_SLUG = ENTRIES.stream().collect(Collectors.toMap(e -> Slugs.of(e.name()), e -> e, (a, b) -> a));
    }

    private static void add(List<Entry> list, int[] pos, String name, String hebrew, String book) {
        list.add(new Entry(name, hebrew, book, pos[0]++));
    }

    private ParashotCatalog() {
    }

    /** Retrouve une parasha par son nom, sans tenir compte de la casse, des accents ni du type d'apostrophe. */
    public static Entry find(String name) {
        return BY_SLUG.get(Slugs.of(name));
    }
}
