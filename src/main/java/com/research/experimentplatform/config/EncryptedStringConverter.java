package com.research.experimentplatform.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// Conversor JPA: cuando JPA escribe en BD cifra el valor,
// cuando lee de BD lo descifra automáticamente.
// Se aplica con @Convert(converter = EncryptedStringConverter.class)
// en los campos que contengan datos personales.
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return EncryptionUtils.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return EncryptionUtils.decrypt(dbData);
    }
}
