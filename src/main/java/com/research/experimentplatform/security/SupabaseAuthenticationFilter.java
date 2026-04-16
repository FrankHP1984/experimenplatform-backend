package com.research.experimentplatform.security;

import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.research.experimentplatform.model.User;
import com.research.experimentplatform.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class SupabaseAuthenticationFilter extends OncePerRequestFilter {

    private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;
    private final UserRepository userRepository;

    public SupabaseAuthenticationFilter(ConfigurableJWTProcessor<SecurityContext> jwtProcessor,
                                        UserRepository userRepository) {
        this.jwtProcessor = jwtProcessor;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                JWTClaimsSet claims = jwtProcessor.process(token, null);

                String supabaseId = claims.getSubject();
                String email = (String) claims.getClaim("email");

                if (supabaseId != null && email != null) {
                    // El claim "role" del JWT de Supabase es siempre "authenticated".
                    // El rol real de la app se obtiene de la DB local.
                    Optional<User> user = userRepository.findBySupabaseId(supabaseId);
                    List<SimpleGrantedAuthority> authorities = user
                            .map(u -> List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())))
                            .orElse(List.of());

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(email, null, authorities);
                    authentication.setDetails(supabaseId);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                logger.warn("Invalid Supabase token: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
