package com.chemindeguer.backend.fete;

import com.chemindeguer.backend.common.Slugs;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Les fêtes et jours particuliers du calendrier juif, dans l'ordre du calendrier, avec les noms d'origine utilisés par Hebcal.
 * Sert à classer les résumés, à les relier au calendrier de l'accueil et à calculer leur prochaine date.
 */
public final class FetesCatalog {
    /**
     * @param name        nom affiché
     * @param hebrew      nom en hébreu
     * @param category    catégorie de regroupement
     * @param position    ordre dans le calendrier
     * @param hebcalKeys  noms Hebcal normalisés ; le premier sert à calculer la date, les suivants (veille...) seulement à relier
     */
    public record Entry(String name, String hebrew, String category, int position, List<String> hebcalKeys) {
        public String slug() {
            return Slugs.of(name);
        }

        public String primaryKey() {
            return hebcalKeys.get(0);
        }
    }

    public static final String OTHER_CATEGORY = "Autres";
    public static final int OTHER_POSITION = 999;

    public static final List<Entry> ENTRIES;
    private static final Map<String, Entry> BY_SLUG = new LinkedHashMap<>();
    private static final Map<String, Entry> BY_HEBCAL_KEY = new LinkedHashMap<>();

    static {
        List<Entry> list = new ArrayList<>();
        int[] pos = {0};
        add(list, pos, "Roch Hachana", "ראש השנה", "Fêtes de Tichri", "rosh hashana", "erev rosh hashana");
        add(list, pos, "Yom Kippour", "יום כיפור", "Fêtes de Tichri", "yom kippur", "erev yom kippur");
        add(list, pos, "Soukkot", "סוכות", "Fêtes de Tichri", "sukkot", "erev sukkot");
        add(list, pos, "Chemini Atséret", "שמיני עצרת", "Fêtes de Tichri", "shmini atzeret");
        add(list, pos, "Sim'hat Torah", "שמחת תורה", "Fêtes de Tichri", "simchat torah");
        add(list, pos, "Hanoucca", "חנוכה", "Hiver et printemps", "chanukah");
        add(list, pos, "Tou BiChvat", "ט״ו בשבט", "Hiver et printemps", "tu bishvat");
        add(list, pos, "Pourim", "פורים", "Hiver et printemps", "purim", "erev purim", "shushan purim");
        add(list, pos, "Pessah", "פסח", "Hiver et printemps", "pesach", "erev pesach");
        add(list, pos, "Lag BaOmer", "ל״ג בעומר", "Omer et Chavouot", "lag ba'omer");
        add(list, pos, "Chavouot", "שבועות", "Omer et Chavouot", "shavuot", "erev shavuot");
        add(list, pos, "Tou BeAv", "ט״ו באב", "Été", "tu b'av");
        add(list, pos, "Jeûne de Guédalia", "צום גדליה", "Jeûnes", "tzom gedaliah");
        add(list, pos, "Assara BeTévet", "עשרה בטבת", "Jeûnes", "asara b'tevet");
        add(list, pos, "Jeûne d'Esther", "תענית אסתר", "Jeûnes", "ta'anit esther");
        add(list, pos, "Jeûne du 17 Tamouz", "שבעה עשר בתמוז", "Jeûnes", "tzom tammuz");
        add(list, pos, "Tich'a BeAv", "תשעה באב", "Jeûnes", "tish'a b'av", "erev tish'a b'av");
        add(list, pos, "Roch 'Hodech", "ראש חודש", "Chaque mois", "rosh chodesh");
        ENTRIES = List.copyOf(list);
        for (Entry e : ENTRIES) {
            BY_SLUG.put(e.slug(), e);
            for (String k : e.hebcalKeys()) BY_HEBCAL_KEY.put(k, e);
        }
    }

    private static void add(List<Entry> list, int[] pos, String name, String hebrew, String category, String... keys) {
        list.add(new Entry(name, hebrew, category, pos[0]++, List.of(keys)));
    }

    private FetesCatalog() {
    }

    public static Entry find(String name) {
        return BY_SLUG.get(Slugs.of(name));
    }

    public static Entry bySlug(String slug) {
        return BY_SLUG.get(slug);
    }

    /** Fête correspondant à un nom d'origine Hebcal (par exemple « Pesach III (CH''M) »), si elle figure au catalogue. */
    public static Optional<Entry> matchHebcal(String titleOrig) {
        return Optional.ofNullable(BY_HEBCAL_KEY.get(normalizeKey(titleOrig)));
    }

    /** « Rosh Hashana 5787 », « Chanukah: 3 Candles », « Sukkot VII (Hoshana Raba) »... deviennent une clé stable. */
    public static String normalizeKey(String titleOrig) {
        if (titleOrig == null) return "";
        String s = Normalizer.normalize(titleOrig, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
        s = s.replaceAll("\\s*\\(.*?\\)", "").split(":")[0];
        s = s.replaceAll("\\s*\\d{4}$", "").replaceAll("\\s+[ivx]+$", "").trim();
        return s.startsWith("rosh chodesh") ? "rosh chodesh" : s;
    }
}
