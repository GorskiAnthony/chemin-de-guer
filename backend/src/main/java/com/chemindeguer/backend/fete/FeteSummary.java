package com.chemindeguer.backend.fete;

import com.chemindeguer.backend.article.StatutArticle;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class FeteSummary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(length = 80)
    private String hebrewName;

    @Column(nullable = false, length = 40)
    private String category;

    @Column(nullable = false)
    private int position;

    @Column(columnDefinition = "TEXT")
    private String intro;

    @ElementCollection
    @CollectionTable(name = "fete_section", joinColumns = @JoinColumn(name = "fete_id"))
    @OrderColumn(name = "idx")
    private List<FeteSection> sections = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutArticle statut;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;
}
