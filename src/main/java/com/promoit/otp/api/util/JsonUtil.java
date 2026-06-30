package com.promoit.otp.api.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;

/**
 * Thin wrapper around Jackson for the HTTP layer: parses request bodies into a
 * tree and serialises responses.
 */
public class JsonUtil {

    private final ObjectMapper mapper;

    public JsonUtil() {
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /** Reads a JSON request body; an empty body yields an empty object node. */
    public JsonNode readTree(InputStream in) {
        try {
            if (in == null) {
                return mapper.createObjectNode();
            }
            byte[] bytes = in.readAllBytes();
            if (bytes.length == 0) {
                return mapper.createObjectNode();
            }
            return mapper.readTree(bytes);
        } catch (IOException e) {
            throw com.promoit.otp.ApiException.badRequest("Malformed JSON body");
        }
    }

    public byte[] toBytes(Object value) {
        try {
            return mapper.writeValueAsBytes(value);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialise response", e);
        }
    }
}
