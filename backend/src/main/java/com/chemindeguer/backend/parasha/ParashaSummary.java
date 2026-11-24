package com.chemindeguer.backend.parasha;

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
public class ParashaSummary {
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
    private String book;

    @Column(nullable = false)
    private int position;

    @Column(columnDefinition = "TEXT")
    private String intro;

    @ElementCollection
    @CollectionTable(name = "parasha_aliyah", joinColumns = @JoinColumn(name = "parasha_id"))
    @OrderColumn(name = "idx")
    private List<Aliyah> aliyot = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutArticle statut;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;
}
