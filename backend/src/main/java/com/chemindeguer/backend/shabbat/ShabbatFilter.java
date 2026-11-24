package com.chemindeguer.backend.shabbat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Pendant Shabbat, le contenu public est remplacé par une réponse 503. L'administration reste accessible. */
@Component
public class ShabbatFilter extends OncePerRequestFilter {
    private final ShabbatService shabbatService;

    public ShabbatFilter(ShabbatService shabbatService) {
        this.shabbatService = shabbatService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean publicContent = path.startsWith("/api/public/") || path.equals("/api/sitemap.xml") || path.equals("/api/rss.xml");
        return !publicContent || path.startsWith("/api/public/shabbat");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (shabbatService.isClosed()) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"closed\":true,\"message\":\"Site fermé pour Shabbat\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
