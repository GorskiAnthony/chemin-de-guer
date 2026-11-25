package com.chemindeguer.backend.stats;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.HexFormat;

/** Empreinte anonyme d'un visiteur : le sel est tiré au sort chaque jour, gardé en mémoire seulement, puis oublié. */
@Component
public class VisitorHasher {
    private final SecureRandom random = new SecureRandom();
    private LocalDate saltDay;
    private byte[] salt;

    public synchronized String hash(LocalDate day, String ip, String userAgent) {
        if (!day.equals(saltDay)) {
            salt = new byte[32];
            random.nextBytes(salt);
            saltDay = day;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            digest.update((ip + "|" + userAgent).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
