package com.chemindeguer.backend.shabbat;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration de la fermeture pour Shabbat.
 *
 * @param enabled       active la fermeture automatique
 * @param geonameId     identifiant GeoNames de la ville de référence (Paris : 2988507)
 * @param timezone      fuseau horaire de la ville
 * @param marginMinutes marge de sécurité appliquée avant l'entrée du Shabbat
 * @param apiUrl        URL de l'API Hebcal
 */
@ConfigurationProperties(prefix = "shabbat")
public record ShabbatProperties(boolean enabled, long geonameId, String timezone, int marginMinutes, String apiUrl) {
    public ShabbatProperties {
        if (timezone == null || timezone.isBlank()) timezone = "Europe/Paris";
        if (apiUrl == null || apiUrl.isBlank()) apiUrl = "https://www.hebcal.com/shabbat";
        if (geonameId == 0) geonameId = 2988507L;
        if (marginMinutes < 0) marginMinutes = 0;
    }
}
