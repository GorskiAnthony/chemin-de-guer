package com.chemindeguer.backend.seo;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Quand le site n'est pas indexable, ajoute X-Robots-Tag: noindex aux réponses des pages publiques et de leurs fichiers
 * (pages rendues, images de partage, flux). L'API JSON et l'administration reçoivent déjà cet en-tête de nginx.
 */
@Component
public class RobotsHeaderFilter extends OncePerRequestFilter {
    private final SiteProperties site;

    public RobotsHeaderFilter(SiteProperties site) {
        this.site = site;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (site.indexable()) return true;
        String p = request.getRequestURI();
        return !(p.startsWith("/api/seo/") || p.startsWith("/api/og/") || p.equals("/api/sitemap.xml")
                || p.equals("/api/rss.xml") || p.startsWith("/api/llms"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("X-Robots-Tag", SeoPageService.ROBOTS_NOINDEX);
        chain.doFilter(request, response);
    }
}
