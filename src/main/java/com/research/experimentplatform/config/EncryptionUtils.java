package com.research.experimentplatform.config;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

// Utilidad estática para cifrar y descifrar textos con AES-256-CBC.
// Se inicializa una sola vez al arrancar la aplicación desde EncryptionConfig.
// El IV es fijo (derivado de la clave) para que el mismo texto siempre produzca
// el mismo cifrado, lo que permite hacer búsquedas en la base de datos.
public class EncryptionUtils {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static byte[] keyBytes;
    private static byte[] ivBytes;

    public static void init(String encryptionKey) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            // La clave AES son los 32 bytes del SHA-256 de la clave de entorno
            keyBytes = sha.digest(encryptionKey.getBytes(StandardCharsets.UTF_8));
            // El IV son los primeros 16 bytes del SHA-256 de (clave + "IV")
            ivBytes = Arrays.copyOf(
                    sha.digest((encryptionKey + "IV").getBytes(StandardCharsets.UTF_8)), 16);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize encryption", e);
        }
    }

    public static String encrypt(String value) {
        if (value == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(keyBytes, "AES"),
                    new IvParameterSpec(ivBytes));
            return Base64.getEncoder().encodeToString(
                    cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public static String decrypt(String value) {
        if (value == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(keyBytes, "AES"),
                    new IvParameterSpec(ivBytes));
            return new String(cipher.doFinal(Base64.getDecoder().decode(value)),
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
