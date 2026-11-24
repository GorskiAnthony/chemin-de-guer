package com.chemindeguer.backend.fete;

import com.chemindeguer.backend.article.StatutArticle;
import com.chemindeguer.backend.calendar.CalendarService;
import com.chemindeguer.backend.common.Slugs;
import com.chemindeguer.backend.fete.FeteDtos.FeteRequest;
import com.chemindeguer.backend.fete.FeteDtos.FeteResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class FeteService {
    private final FeteRepository repository;
    private final CalendarService calendar;

    public FeteService(FeteRepository repository, CalendarService calendar) {
        this.repository = repository;
        this.calendar = calendar;
    }

    @Transactional(readOnly = true)
    public List<FeteResponse> publicList() {
        return repository.findAllByStatutOrderByPositionAscNameAsc(StatutArticle.PUBLISHED).stream()
                .map(f -> FeteResponse.from(f, null).summary()).toList();
    }

    @Transactional(readOnly = true)
    public FeteResponse publicBySlug(String slug) {
        return repository.findBySlugAndStatut(slug, StatutArticle.PUBLISHED)
                .map(f -> FeteResponse.from(f, calendar.nextOccurrence(f.getSlug())))
                .orElseThrow(() -> new NoSuchElementException("Fête non trouvée"));
    }

    @Transactional(readOnly = true)
    public List<FeteResponse> adminList() {
        return repository.findAllByOrderByPositionAscNameAsc().stream().map(f -> FeteResponse.from(f, null).summary()).toList();
    }

    @Transactional(readOnly = true)
    public FeteResponse adminGet(Long id) {
        return repository.findById(id).map(f -> FeteResponse.from(f, null))
                .orElseThrow(() -> new NoSuchElementException("Fête non trouvée"));
    }

    @Transactional
    public FeteResponse create(FeteRequest request) {
        var f = new FeteSummary();
        f.setCreatedAt(Instant.now());
        return save(f, request);
    }

    @Transactional
    public FeteResponse update(Long id, FeteRequest request) {
        var f = repository.findById(id).orElseThrow(() -> new NoSuchElementException("Fête non trouvée"));
        return save(f, request);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("Fête non trouvée");
        }
        repository.deleteById(id);
    }

    private FeteResponse save(FeteSummary f, FeteRequest r) {
        String slug = Slugs.of(r.name());
        repository.findBySlug(slug).filter(other -> !other.getId().equals(f.getId())).ifPresent(other -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette fête existe déjà");
        });
        var entry = FetesCatalog.find(r.name());
        f.setName(r.name().trim());
        f.setSlug(slug);
        f.setHebrewName(r.hebrewName() == null || r.hebrewName().isBlank() ? (entry != null ? entry.hebrew() : null) : r.hebrewName().trim());
        f.setCategory(entry != null ? entry.category() : FetesCatalog.OTHER_CATEGORY);
        f.setPosition(entry != null ? entry.position() : FetesCatalog.OTHER_POSITION);
        f.setIntro(r.intro());
        f.getSections().clear();
        if (r.sections() != null) {
            r.sections().stream()
                    .filter(s -> s.content() != null && !s.content().isBlank())
                    .forEach(s -> f.getSections().add(new FeteSection(s.label(), s.content())));
        }
        f.setStatut(r.statut() == null ? StatutArticle.DRAFT : r.statut());
        f.setUpdatedAt(Instant.now());
        return FeteResponse.from(repository.save(f), null);
    }
}
