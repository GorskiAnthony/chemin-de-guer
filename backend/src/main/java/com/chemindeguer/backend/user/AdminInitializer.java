package com.chemindeguer.backend.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Crée le compte administrateur au premier démarrage à partir de ADMIN_EMAIL / ADMIN_PASSWORD. */
@Component
public class AdminInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
                            @Value("${app.admin.email:}") String email, @Value("${app.admin.password:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        if (email.isBlank() || password.length() < 12) {
            log.warn("Aucun administrateur créé : définir ADMIN_EMAIL et ADMIN_PASSWORD (12 caractères minimum).");
            return;
        }
        var admin = new Utilisateur();
        admin.setEmail(email.trim().toLowerCase());
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        log.info("Compte administrateur créé.");
    }
}
