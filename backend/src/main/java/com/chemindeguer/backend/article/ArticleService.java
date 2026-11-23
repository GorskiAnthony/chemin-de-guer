package com.chemindeguer.backend.article;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;

    public ArticleService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    public Article create(Article article) {
        if (article.getStatut() == null) {
            article.setStatut(StatutArticle.DRAFT);
        }
        article.setCreatedAt(Instant.now());
        article.setUpdatedAt(Instant.now());
        return articleRepository.save(article);
    }

    public Optional<Article> findById(Long id) {
        return articleRepository.findById(id);
    }

    public List<Article> findAll() {
        return articleRepository.findAll();
    }

    public Article update(Long id, Article modifications) {
        return articleRepository.findById(id)
                .map(existingArticle -> {
                    existingArticle.setTitle(modifications.getTitle());
                    existingArticle.setContent(modifications.getContent());
                    existingArticle.setUpdatedAt(Instant.now());
                    return articleRepository.save(existingArticle);
                })
                .orElseThrow(() -> new NoSuchElementException("Article not found with id: " + id));
    }

    public void delete(Long id) {
        articleRepository.deleteById(id);
    }
}
