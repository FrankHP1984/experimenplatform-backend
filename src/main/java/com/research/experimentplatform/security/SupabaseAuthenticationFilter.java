package com.research.experimentplatform.security;

import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.research.experimentplatform.model.User;
import com.research.experimentplatform.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URL;
import java.security.interfaces.ECPublicKey;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Component
public class SupabaseAuthenticationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Value("${supabase.url}")
    private String supabaseUrl;

    private ECPublicKey ecPublicKey;

    public SupabaseAuthenticationFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void init() {
        loadEcPublicKey();
    }

    private synchronized void loadEcPublicKey() {
        try {
            URL jwksUrl = new URL(supabaseUrl + "/auth/v1/.well-known/jwks.json");
            JWKSet jwkSet = JWKSet.load(jwksUrl);
            ECKey ecKey = (ECKey) jwkSet.getKeys().get(0);
            ecPublicKey = ecKey.toECPublicKey();
            logger.info("Clave pública EC cargada desde JWKS: " + supabaseUrl);
        } catch (Exception e) {
            logger.error("No se pudo cargar JWKS al iniciar: " + e.getMessage());
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                // Lazy load si el init falló
                if (ecPublicKey == null) {
                    loadEcPublicKey();
                }
                if (ecPublicKey == null) {
                    logger.error("Clave EC no disponible, rechazando token");
                    filterChain.doFilter(request, response);
                    return;
                }

                SignedJWT signedJWT = SignedJWT.parse(token);

                ECDSAVerifier verifier = new ECDSAVerifier(ecPublicKey);
                if (!signedJWT.verify(verifier)) {
                    logger.warn("Firma JWT inválida");
                    filterChain.doFilter(request, response);
                    return;
                }

                JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

                // Verificar expiración
                Date expiration = claims.getExpirationTime();
                if (expiration != null && expiration.before(new Date())) {
                    logger.warn("Token JWT expirado");
                    filterChain.doFilter(request, response);
                    return;
                }

                String supabaseId = claims.getSubject();
                String email = (String) claims.getClaim("email");

                if (supabaseId != null) {
                    Optional<User> user = userRepository.findBySupabaseId(supabaseId);
                    List<SimpleGrantedAuthority> authorities = user
                            .map(u -> List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())))
                            .orElseGet(() -> List.of(new SimpleGrantedAuthority("ROLE_AUTHENTICATED")));

                    String principal = email != null ? email : supabaseId;
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    authentication.setDetails(supabaseId);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.info("Autenticación OK para: " + principal);
                }

            } catch (Exception e) {
                logger.error("Error validando token JWT: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
