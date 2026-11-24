package com.chemindeguer.backend.article;

import org.springframework.web.bind.annotation.*;

import java.util.List;

/** API publique : uniquement les articles publiés, sans email d'auteur. */
@RestController
@RequestMapping("/api/public/articles")
public class PublicArticleController {

    private final ArticleService articleService;

    public PublicArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @GetMapping
    public List<ArticleResponse> list(@RequestParam(required = false) String tag,
                                      @RequestParam(required = false) String q) {
        return articleService.searchPublished(tag, q);
    }

    @GetMapping("/{id}")
    public ArticleResponse get(@PathVariable Long id) {
        return articleService.findPublishedById(id);
    }

    @GetMapping("/{id}/related")
    public List<ArticleResponse> related(@PathVariable Long id) {
        return articleService.related(id);
    }
}
