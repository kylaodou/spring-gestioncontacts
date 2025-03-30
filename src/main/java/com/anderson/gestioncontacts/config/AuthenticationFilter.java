package com.anderson.gestioncontacts.config;

import com.anderson.gestioncontacts.model.User;
import com.anderson.gestioncontacts.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class AuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);
    private static final List<String> PUBLIC_ENDPOINTS = List.of("/api/auth/register", "/api/auth/login");

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Autowired
    public AuthenticationFilter(@Lazy UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();
        logger.info("Traitement de la requête pour le chemin : {}", path);
        String requestPath = request.getServletPath();
        if (requestPath.startsWith("/api/public/") || requestPath.startsWith("/api/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }
        // Ignorer les endpoints publics
        if (PUBLIC_ENDPOINTS.contains(path)) {
            logger.info("Endpoint public, accès autorisé sans token");
            filterChain.doFilter(request, response);
            return;
        }

        // Récupérer le token depuis l'en-tête Authorization
        String token = request.getHeader("Authorization");

        if (token != null && token.startsWith("Bearer ")) {
            try {
                // Extraire le token sans le préfixe "Bearer "
                token = token.substring(7);

                // Vérifier si le token est valide
                if (!jwtUtil.validateToken(token)) {
                    sendUnauthorizedResponse(response, "Token invalide ou expiré");
                    return;
                }

                // Extraire l'email du token
                String email = jwtUtil.extractEmail(token);
                if (email == null) {
                    sendUnauthorizedResponse(response, "Token mal formé");
                    return;
                }

                // Vérifier si l'utilisateur existe
                Optional<User> optionalUser = userService.findByEmail(email);
                if (optionalUser.isEmpty()) {
                    sendUnauthorizedResponse(response, "Utilisateur non trouvé");
                    return;
                }

                User user = optionalUser.get();
                logger.info("Utilisateur authentifié : {}", user.getEmail());

                // Créer l'objet d'authentification Spring Security
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        user.getEmail(),
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("USER"))
                );

                // Définir l'authentification dans le contexte de sécurité
                SecurityContextHolder.getContext().setAuthentication(authentication);
                filterChain.doFilter(request, response);
            } catch (Exception e) {
                logger.error("Erreur lors de l'authentification", e);
                sendUnauthorizedResponse(response, "Erreur d'authentification");
            }
        } else {
            sendUnauthorizedResponse(response, "Token non valide ou manquant");
        }
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}