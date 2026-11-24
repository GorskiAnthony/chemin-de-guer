package com.chemindeguer.backend.article;

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

    @Transactional(readOnly = true)
    public ArticleResponse findPublishedById(Long id) {
        return articleRepository.findPublishedById(id).map(a -> ArticleResponse.from(a).publicView())
                .orElseThrow(() -> new NoSuchElementException("Article non trouvé"));
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
