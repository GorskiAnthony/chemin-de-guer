package com.chemindeguer.backend.tag;

import com.chemindeguer.backend.common.Slugs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class TagService {
    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional(readOnly = true)
    public List<Tag> findAll() {
        return tagRepository.findAllByOrderByNameAsc();
    }

    @Transactional
    public Tag create(String name) {
        String slug = Slugs.of(name);
        return tagRepository.findBySlug(slug).orElseGet(() -> tagRepository.save(new Tag(name.trim(), slug)));
    }

    @Transactional
    public void delete(Long id) {
        if (!tagRepository.existsById(id)) {
            throw new NoSuchElementException("Tag non trouvé");
        }
        tagRepository.deleteById(id);
    }
}
