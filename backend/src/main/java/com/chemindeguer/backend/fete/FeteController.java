package com.chemindeguer.backend.fete;

import com.chemindeguer.backend.fete.FeteDtos.FeteRequest;
import com.chemindeguer.backend.fete.FeteDtos.FeteResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class FeteController {
    /** Entrée du catalogue exposée à l'éditeur (liste de suggestions). */
    public record CatalogEntry(String name, String hebrew, String category, int position) {
    }

    private final FeteService service;

    public FeteController(FeteService service) {
        this.service = service;
    }

    @GetMapping("/api/public/fetes")
    public List<FeteResponse> publicList() {
        return service.publicList();
    }

    @GetMapping("/api/public/fetes/catalog")
    public List<CatalogEntry> catalog() {
        return FetesCatalog.ENTRIES.stream().map(e -> new CatalogEntry(e.name(), e.hebrew(), e.category(), e.position())).toList();
    }

    @GetMapping("/api/public/fetes/{slug}")
    public FeteResponse publicBySlug(@PathVariable String slug) {
        return service.publicBySlug(slug);
    }

    @GetMapping("/api/fetes")
    public List<FeteResponse> adminList() {
        return service.adminList();
    }

    @GetMapping("/api/fetes/{id}")
    public FeteResponse adminGet(@PathVariable Long id) {
        return service.adminGet(id);
    }

    @PostMapping("/api/fetes")
    @ResponseStatus(HttpStatus.CREATED)
    public FeteResponse create(@Valid @RequestBody FeteRequest request) {
        return service.create(request);
    }

    @PutMapping("/api/fetes/{id}")
    public FeteResponse update(@PathVariable Long id, @Valid @RequestBody FeteRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/api/fetes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
