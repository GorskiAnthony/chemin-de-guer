package com.chemindeguer.backend.article;

import com.chemindeguer.backend.tag.Tag;
import com.chemindeguer.backend.user.Utilisateur;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    /** Adresse lisible de l'article. Nullable en base pour les articles antérieurs, renseignée au démarrage. */
    @Column(unique = true, length = 100)
    private String slug;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Utilisateur author;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant publishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutArticle statut;

    /** Commentaires ouverts sur cet article (par défaut oui). Nullable en base pour les articles antérieurs. */
    @Column(name = "comments_open", columnDefinition = "boolean default true")
    private Boolean commentsOpen = Boolean.TRUE;

    @ManyToMany
    @JoinTable(name = "article_tag",
            joinColumns = @JoinColumn(name = "article_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    public boolean commentsAllowed() {
        return commentsOpen == null || commentsOpen;
    }

    public Article(String title, String content, Utilisateur author) {
        this.title = title;
        this.content = content;
        this.author = author;
    }
}
