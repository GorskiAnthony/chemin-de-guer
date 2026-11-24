package com.chemindeguer.backend.settings;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Réglages du site (administration uniquement : toute route /api non publique exige une session). */
@RestController
@RequestMapping("/api/settings")
public class SettingsController {
    public record SettingsDto(boolean commentsEnabled) {
    }

    private final SettingsService settings;

    public SettingsController(SettingsService settings) {
        this.settings = settings;
    }

    @GetMapping
    public SettingsDto get() {
        return new SettingsDto(settings.commentsEnabled());
    }

    @PutMapping
    public SettingsDto update(@RequestBody SettingsDto dto) {
        settings.setCommentsEnabled(dto.commentsEnabled());
        return new SettingsDto(settings.commentsEnabled());
    }
}
