package com.chemindeguer.backend.stats;

import com.chemindeguer.backend.common.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class StatsController {
    public record HitRequest(@Size(max = 200) String path, @Size(max = 500) String referrer) {}

    private final StatsService stats;

    public StatsController(StatsService stats) {
        this.stats = stats;
    }

    /**
     * Compte une page vue. Ignorée sans erreur pour : le visiteur qui refuse le suivi (DNT ou GPC), l'administrateur connecté,
     * les robots et toute page hors du site public.
     */
    @PostMapping("/api/public/hit")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void hit(@Valid @RequestBody HitRequest request, HttpServletRequest http, Authentication authentication) {
        boolean admin = authentication != null && !(authentication instanceof AnonymousAuthenticationToken);
        boolean refuses = "1".equals(http.getHeader("DNT")) || "1".equals(http.getHeader("Sec-GPC"));
        if (admin || refuses) return;
        stats.record(request.path(), request.referrer(), http.getHeader("User-Agent"), ClientIp.of(http));
    }

    @GetMapping("/api/stats")
    public StatsService.Summary summary(@RequestParam(defaultValue = "30") int days) {
        return stats.summary(days);
    }
}
