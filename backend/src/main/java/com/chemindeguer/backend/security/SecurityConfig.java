package com.chemindeguer.backend.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           @Value("${server.servlet.session.cookie.secure:false}") boolean cookieSecure) throws Exception {
        // Derrière un reverse proxy HTTPS, la requête arrive en HTTP : le drapeau Secure est donc forcé par configuration.
        CookieCsrfTokenRepository csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepository.setCookieCustomizer(cookie -> cookie.secure(cookieSecure).sameSite("Lax"));
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        // GET et HEAD : certains robots et outils d'aperçu vérifient d'abord les en-têtes avec HEAD
                        .requestMatchers(HttpMethod.GET, "/api/public/**", "/api/sitemap.xml", "/api/rss.xml", "/api/llms.txt", "/api/llms-full.txt",
                                "/api/seo/**", "/api/og/**", "/api/robots.txt").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/api/public/**", "/api/sitemap.xml", "/api/rss.xml", "/api/llms.txt", "/api/llms-full.txt",
                                "/api/seo/**", "/api/og/**", "/api/robots.txt").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/public/articles/*/comments").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers("/api/auth/login"))
                // Force l'écriture du cookie XSRF-TOKEN dès la première réponse (nécessaire pour la SPA).
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .formLogin(form -> form.disable())
                .headers(headers -> headers
                        .contentTypeOptions(c -> {})
                        .frameOptions(f -> f.deny()))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED))
                );
        return http.build();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
