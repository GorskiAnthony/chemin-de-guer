package com.chemindeguer.backend.article;

import com.chemindeguer.backend.common.Slugs;
import com.chemindeguer.backend.tag.Tag;
import com.chemindeguer.backend.tag.TagRepository;
import com.chemindeguer.backend.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;

    public ArticleService(ArticleRepository articleRepository, TagRepository tagRepository, UserRepository userRepository) {
        this.articleRepository = articleRepository;
        this.tagRepository = tagRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ArticleResponse create(ArticleRequest request, String authorEmail) {
        var author = userRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new NoSuchElementException("Utilisateur non trouvé"));
        var article = new Article(request.title().trim(), request.content(), author);
        Instant now = Instant.now();
        article.setCreatedAt(now);
        article.setUpdatedAt(now);
        article.setSlug(uniqueSlug(request.slug() == null || request.slug().isBlank() ? request.title() : request.slug(), null));
        applyStatut(article, request.statut());
        article.setTags(resolveTags(request.tagIds()));
        return ArticleResponse.from(articleRepository.save(article));
    }

    @Transactional
    public ArticleResponse update(Long id, ArticleRequest request) {
        var article = articleRepository.findByIdWithAuthor(id)
                .orElseThrow(() -> new NoSuchElementException("Article non trouvé"));
        article.setTitle(request.title().trim());
        article.setContent(request.content());
        article.setUpdatedAt(Instant.now());
        // Le slug ne change pas avec le titre (les liens existants restent valides) : uniquement s'il est modifié explicitement
        if (request.slug() != null && !request.slug().isBlank() && !request.slug().equals(article.getSlug())) {
            article.setSlug(uniqueSlug(request.slug(), article.getId()));
        } else if (article.getSlug() == null) {
            article.setSlug(uniqueSlug(request.title(), article.getId()));
        }
        applyStatut(article, request.statut());
        article.setTags(resolveTags(request.tagIds()));
        return ArticleResponse.from(article);
    }

    @Transactional
    public void delete(Long id) {
        if (!articleRepository.existsById(id)) {
            throw new NoSuchElementException("Article non trouvé");
        }
        articleRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public ArticleResponse findById(Long id) {
        return articleRepository.findByIdWithAuthor(id).map(ArticleResponse::from)
                .orElseThrow(() -> new NoSuchElementException("Article non trouvé"));
    }

    @Transactional(readOnly = true)
    public List<ArticleResponse> findAll() {
        return articleRepository.findAllWithAuthor().stream().map(ArticleResponse::from).toList();
    }

    /** Accepte un slug, ou un identifiant numérique (anciens liens /articles/1). */
    @Transactional(readOnly = true)
    public ArticleResponse findPublishedByKey(String key) {
        var found = key.matches("\\d{1,18}")
                ? articleRepository.findPublishedById(Long.parseLong(key))
                : articleRepository.findPublishedBySlug(key);
        return found.map(a -> ArticleResponse.from(a).publicView())
                .orElseThrow(() -> new NoSuchElementException("Article non trouvé"));
    }

    /** Renseigne le slug des articles créés avant son introduction. */
    @Transactional
    public int backfillSlugs() {
        var missing = articleRepository.findAllBySlugIsNull();
        missing.forEach(a -> a.setSlug(uniqueSlug(a.getTitle(), a.getId())));
        return missing.size();
    }

    private String uniqueSlug(String text, Long selfId) {
        String base = Slugs.forArticle(text);
        String candidate = base;
        int i = 2;
        while (articleRepository.findBySlug(candidate).filter(a -> !a.getId().equals(selfId)).isPresent()) {
            String suffix = "-" + i++;
            candidate = (base.length() + suffix.length() > 100 ? base.substring(0, 100 - suffix.length()) : base) + suffix;
        }
        return candidate;
    }

    @Transactional(readOnly = true)
    public List<ArticleResponse> searchPublished(String tagSlug, String query) {
        String like = toLikePattern(query);
        List<Article> found = (tagSlug == null || tagSlug.isBlank())
                ? articleRepository.searchPublished(like)
                : articleRepository.searchPublishedByTag(tagSlug, like);
        return found.stream().map(a -> ArticleResponse.from(a).summary()).toList();
    }

    /** Articles publiés liés : même tag, hors article courant (issue #29). */
    @Transactional(readOnly = true)
    public List<ArticleResponse> related(Long id) {
        var current = articleRepository.findPublishedById(id)
                .orElseThrow(() -> new NoSuchElementException("Article non trouvé"));
        Set<Long> seen = new HashSet<>();
        seen.add(id);
        return current.getTags().stream()
                .flatMap(t -> articleRepository.searchPublishedByTag(t.getSlug(), "%").stream())
                .filter(a -> seen.add(a.getId()))
                .limit(3)
                .map(a -> ArticleResponse.from(a).summary())
                .toList();
    }

    static String toLikePattern(String query) {
        if (query == null || query.isBlank()) {
            return "%";
        }
        String escaped = query.trim().toLowerCase()
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private void applyStatut(Article article, StatutArticle requested) {
        StatutArticle statut = requested == null ? StatutArticle.DRAFT : requested;
        if (statut == StatutArticle.PUBLISHED && article.getPublishedAt() == null) {
            article.setPublishedAt(Instant.now());
        }
        article.setStatut(statut);
    }

    private Set<Tag> resolveTags(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(tagRepository.findAllById(tagIds));
    }
}
