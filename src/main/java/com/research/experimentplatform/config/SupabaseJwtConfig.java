package com.research.experimentplatform.config;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.URL;
import java.util.Set;

@Configuration
public class SupabaseJwtConfig {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Bean
    public ConfigurableJWTProcessor<SecurityContext> jwtProcessor() throws Exception {
        ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();


        URL jwksUrl = URI.create(supabaseUrl + "/auth/v1/.well-known/jwks.json").toURL();

        JWKSource<SecurityContext> keySource = JWKSourceBuilder
                .create(jwksUrl)
                .build();

        JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(
                Set.of(JWSAlgorithm.RS256), keySource
        );

        processor.setJWSKeySelector(keySelector);
        return processor;
    }
}
