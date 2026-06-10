package com.forgeStackk.EduResolve.dto.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Accepts both a JSON string ("whatsapp") and a JSON array (["in-app","whatsapp"])
 * and normalises to a comma-joined String.
 */
public class StringOrListDeserializer extends StdDeserializer<String> {

    public StringOrListDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        if (p.currentToken() == JsonToken.START_ARRAY) {
            List<String> items = new ArrayList<>();
            while (p.nextToken() != JsonToken.END_ARRAY) {
                items.add(p.getText());
            }
            return String.join(",", items);
        }
        return p.getValueAsString();
    }
}
