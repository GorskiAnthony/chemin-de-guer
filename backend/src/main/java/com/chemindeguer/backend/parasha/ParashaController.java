package com.chemindeguer.backend.parasha;

import com.chemindeguer.backend.parasha.ParashaDtos.ParashaRequest;
import com.chemindeguer.backend.parasha.ParashaDtos.ParashaResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ParashaController {
    private final ParashaService service;

    public ParashaController(ParashaService service) {
        this.service = service;
    }

    @GetMapping("/api/public/parashot")
    public List<ParashaResponse> publicList() {
        return service.publicList();
    }

    @GetMapping("/api/public/parashot/catalog")
    public List<ParashotCatalog.Entry> catalog() {
        return ParashotCatalog.ENTRIES;
    }

    @GetMapping("/api/public/parashot/{slug}")
    public ParashaResponse publicBySlug(@PathVariable String slug) {
        return service.publicBySlug(slug);
    }

    @GetMapping("/api/parashot")
    public List<ParashaResponse> adminList() {
        return service.adminList();
    }

    @GetMapping("/api/parashot/{id}")
    public ParashaResponse adminGet(@PathVariable Long id) {
        return service.adminGet(id);
    }

    @PostMapping("/api/parashot")
    @ResponseStatus(HttpStatus.CREATED)
    public ParashaResponse create(@Valid @RequestBody ParashaRequest request) {
        return service.create(request);
    }

    @PutMapping("/api/parashot/{id}")
    public ParashaResponse update(@PathVariable Long id, @Valid @RequestBody ParashaRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/api/parashot/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
