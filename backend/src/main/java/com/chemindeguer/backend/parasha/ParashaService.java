package com.chemindeguer.backend.parasha;

import com.chemindeguer.backend.article.StatutArticle;
import com.chemindeguer.backend.common.Slugs;
import com.chemindeguer.backend.parasha.ParashaDtos.ParashaRequest;
import com.chemindeguer.backend.parasha.ParashaDtos.ParashaResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ParashaService {
    private final ParashaRepository repository;

    public ParashaService(ParashaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ParashaResponse> publicList() {
        return repository.findAllByStatutOrderByPositionAsc(StatutArticle.PUBLISHED).stream()
                .map(p -> ParashaResponse.from(p).summary()).toList();
    }

    @Transactional(readOnly = true)
    public ParashaResponse publicBySlug(String slug) {
        return repository.findBySlugAndStatut(slug, StatutArticle.PUBLISHED).map(ParashaResponse::from)
                .orElseThrow(() -> new NoSuchElementException("Paracha non trouvée"));
    }

    @Transactional(readOnly = true)
    public List<ParashaResponse> adminList() {
        return repository.findAllByOrderByPositionAsc().stream().map(p -> ParashaResponse.from(p).summary()).toList();
    }

    @Transactional(readOnly = true)
    public ParashaResponse adminGet(Long id) {
        return repository.findById(id).map(ParashaResponse::from)
                .orElseThrow(() -> new NoSuchElementException("Paracha non trouvée"));
    }

    @Transactional
    public ParashaResponse create(ParashaRequest request) {
        var p = new ParashaSummary();
        p.setCreatedAt(Instant.now());
        return save(p, request);
    }

    @Transactional
    public ParashaResponse update(Long id, ParashaRequest request) {
        var p = repository.findById(id).orElseThrow(() -> new NoSuchElementException("Paracha non trouvée"));
        return save(p, request);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("Paracha non trouvée");
        }
        repository.deleteById(id);
    }

    private ParashaResponse save(ParashaSummary p, ParashaRequest r) {
        String slug = Slugs.of(r.name());
        repository.findBySlug(slug).filter(other -> !other.getId().equals(p.getId())).ifPresent(other -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette paracha existe déjà");
        });
        var entry = ParashotCatalog.find(r.name());
        p.setName(r.name().trim());
        p.setSlug(slug);
        p.setHebrewName(r.hebrewName() == null || r.hebrewName().isBlank() ? null : r.hebrewName().trim());
        p.setBook(entry != null ? entry.book() : "Autres");
        p.setPosition(entry != null ? entry.position() : 999);
        p.setIntro(r.intro());
        p.getAliyot().clear();
        if (r.aliyot() != null) {
            r.aliyot().stream()
                    .filter(a -> a.content() != null && !a.content().isBlank())
                    .forEach(a -> p.getAliyot().add(new Aliyah(a.label(), a.content())));
        }
        p.setStatut(r.statut() == null ? StatutArticle.DRAFT : r.statut());
        p.setUpdatedAt(Instant.now());
        return ParashaResponse.from(repository.save(p));
    }
}
