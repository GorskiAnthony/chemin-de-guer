package com.chemindeguer.backend.parasha;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** Au démarrage, renseigne le nom hébreu des résumés de paracha créés sans lui, quand la paracha figure au catalogue. */
@Component
public class ParashaHebrewBackfill implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ParashaHebrewBackfill.class);
    private final ParashaRepository repository;
    private final TransactionTemplate tx;

    public ParashaHebrewBackfill(ParashaRepository repository, TransactionTemplate tx) {
        this.repository = repository;
        this.tx = tx;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer updated = tx.execute(status -> {
            int n = 0;
            for (ParashaSummary p : repository.findAll()) {
                if (p.getHebrewName() != null && !p.getHebrewName().isBlank()) continue;
                var entry = ParashotCatalog.find(p.getName());
                if (entry != null) {
                    p.setHebrewName(entry.hebrew());
                    n++;
                }
            }
            return n;
        });
        if (updated != null && updated > 0) log.info("Nom hébreu renseigné pour {} résumé(s) de paracha.", updated);
    }
}
