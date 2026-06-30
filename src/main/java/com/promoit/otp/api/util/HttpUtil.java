package com.promoit.otp.api.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.promoit.otp.ApiException;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Helpers for working with {@link HttpExchange}: reading JSON fields, writing
 * JSON responses and enforcing the request method.
 */
public final class HttpUtil {

    private static final JsonUtil JSON = new JsonUtil();

    private HttpUtil() {
    }

    public static void requireMethod(HttpExchange exchange, String method) {
        if (!exchange.getRequestMethod().equalsIgnoreCase(method)) {
            throw new ApiException(405, "Method not allowed, expected " + method);
        }
    }

    public static JsonNode readBody(HttpExchange exchange) {
        return JSON.readTree(exchange.getRequestBody());
    }

    /** Returns a required, non-blank text field or throws HTTP 400. */
    public static String requiredText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw ApiException.badRequest("Field '" + field + "' is required");
        }
        return value.asText().trim();
    }

    /** Returns an optional text field, or {@code null} when absent/blank. */
    public static String optionalText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            return null;
        }
        return value.asText().trim();
    }

    /** Returns a required integer field or throws HTTP 400. */
    public static int requiredInt(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || !value.canConvertToInt()) {
            throw ApiException.badRequest("Field '" + field + "' must be an integer");
        }
        return value.asInt();
    }

    /** Returns a required long field or throws HTTP 400. */
    public static long requiredLong(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || !value.canConvertToLong()) {
            throw ApiException.badRequest("Field '" + field + "' must be a number");
        }
        return value.asLong();
    }

    public static void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] payload = JSON.toBytes(body);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, payload.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(payload);
        }
    }

    public static void sendError(HttpExchange exchange, int status, String message) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", message);
        body.put("status", status);
        sendJson(exchange, status, body);
    }

    public static Map<String, Object> ok(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        return body;
    }

    /** UTF-8 string body, used rarely; kept for completeness. */
    public static String bodyAsString(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }
}
