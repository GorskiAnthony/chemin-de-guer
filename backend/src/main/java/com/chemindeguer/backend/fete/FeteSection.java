package com.chemindeguer.backend.fete;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Une section du résumé d'une fête (histoire, coutumes, vécu...), rédigée en Markdown. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FeteSection {
    @Column(length = 80)
    private String label;

    @Column(columnDefinition = "TEXT")
    private String content;
}
