package com.chemindeguer.backend.settings;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Réglages modifiables depuis l'administration, sans redéploiement. */
@Service
public class SettingsService {
    static final String COMMENTS_ENABLED = "comments.enabled";

    private final SiteSettingRepository repository;
    private final boolean commentsDefault;

    public SettingsService(SiteSettingRepository repository, @Value("${app.comments.enabled:true}") boolean commentsDefault) {
        this.repository = repository;
        this.commentsDefault = commentsDefault;
    }

    /** Interrupteur général des commentaires. Sans valeur enregistrée, la valeur de COMMENTS_ENABLED s'applique. */
    @Transactional(readOnly = true)
    public boolean commentsEnabled() {
        return repository.findById(COMMENTS_ENABLED).map(s -> Boolean.parseBoolean(s.getValue())).orElse(commentsDefault);
    }

    @Transactional
    public void setCommentsEnabled(boolean enabled) {
        repository.save(new SiteSetting(COMMENTS_ENABLED, Boolean.toString(enabled)));
    }
}
