package com.chemindeguer.backend.article;

import com.chemindeguer.backend.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;
    private final UserRepository userRepository;

    public record ArticleRequest(String title, String content) {}
    public record ArticleResponse(Long id, String title, String content, String authorEmail) {
        public static ArticleResponse from(Article article) {
            return new ArticleResponse(article.getId(), article.getTitle(), article.getContent(), article.getAuthor().getEmail());
        }
    }

    public ArticleController(ArticleService articleService, UserRepository userRepository) {
        this.articleService = articleService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ArticleResponse create(@RequestBody ArticleRequest request) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        var user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        var article = new Article(request.title(), request.content(), user);
        var createdArticle = articleService.create(article);
        return ArticleResponse.from(createdArticle);
    }

    @GetMapping
    public List<ArticleResponse> findAll() {

        return articleService.findAll().stream().map(ArticleResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ArticleResponse findById(@PathVariable Long id) {

        return articleService.findById(id).map(ArticleResponse::from).orElseThrow(() -> new NoSuchElementException("Article non trouvé"));
    }

    @PutMapping("/{id}")
    public ArticleResponse update(@PathVariable Long id, @RequestBody ArticleRequest request) {
        var modifications = new Article(request.title(), request.content(), null);
        var updatedArticle = articleService.update(id, modifications);
        return ArticleResponse.from(updatedArticle);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {

        articleService.delete(id);
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleNotFound() {
    }
}
