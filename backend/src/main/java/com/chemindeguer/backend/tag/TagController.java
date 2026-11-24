package com.chemindeguer.backend.tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TagController {
    public record TagRequest(@NotBlank @Size(max = 60) String name) {
    }

    public record TagResponse(Long id, String name, String slug) {
        public static TagResponse from(Tag t) {
            return new TagResponse(t.getId(), t.getName(), t.getSlug());
        }
    }

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping("/api/public/tags")
    public List<TagResponse> publicTags() {
        return tagService.findAll().stream().map(TagResponse::from).toList();
    }

    @PostMapping("/api/tags")
    public TagResponse create(@Valid @RequestBody TagRequest request) {
        return TagResponse.from(tagService.create(request.name()));
    }

    @DeleteMapping("/api/tags/{id}")
    public void delete(@PathVariable Long id) {
        tagService.delete(id);
    }
}
