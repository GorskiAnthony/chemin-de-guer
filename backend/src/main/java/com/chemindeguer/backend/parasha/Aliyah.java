package com.chemindeguer.backend.parasha;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Une montée (aliyah) de la lecture, avec le résumé écrit par l'auteur. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Aliyah {
    @Column(length = 80)
    private String label;

    @Column(columnDefinition = "TEXT")
    private String content;
}
