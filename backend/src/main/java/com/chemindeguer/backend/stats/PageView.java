package com.chemindeguer.backend.stats;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Une page vue. Aucune donnée personnelle : pas d'adresse IP, pas de cookie. Le « visiteur » est une empreinte
 * calculée avec un sel qui change chaque jour et n'est jamais stocké, donc impossible à relier d'un jour à l'autre.
 */
@Entity
@Table(indexes = @Index(name = "idx_page_view_day", columnList = "day"))
@Getter
@Setter
@NoArgsConstructor
public class PageView {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate day;

    @Column(nullable = false)
    private Instant viewedAt;

    @Column(nullable = false, length = 140)
    private String path;

    @Column(length = 100)
    private String referrerHost;

    @Column(nullable = false, length = 10)
    private String device;

    @Column(nullable = false, length = 64)
    private String visitor;
}
