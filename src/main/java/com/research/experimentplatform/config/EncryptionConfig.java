package com.research.experimentplatform.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EncryptionConfig {

    @Value("${encryption.key}")
    private String encryptionKey;

    // Se ejecuta una vez al arrancar la aplicación.
    // Inicializa EncryptionUtils con la clave del .env para que
    // el conversor JPA pueda cifrar y descifrar emails.
    @PostConstruct
    public void init() {
        EncryptionUtils.init(encryptionKey);
    }
}
