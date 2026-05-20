package com.forgeStackk.EduResolve.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;

@Converter
public class JsonbConverter implements AttributeConverter<String, Object> {

    @Override
    public Object convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        try {
            PGobject pgo = new PGobject();
            pgo.setType("jsonb");
            pgo.setValue(attribute);
            return pgo;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert String to jsonb", e);
        }
    }

    @Override
    public String convertToEntityAttribute(Object dbData) {
        if (dbData == null) return null;
        return dbData.toString();
    }
}
