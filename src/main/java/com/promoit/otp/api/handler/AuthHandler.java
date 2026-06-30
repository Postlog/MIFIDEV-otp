package com.promoit.otp.api.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.promoit.otp.ApiException;
import com.promoit.otp.api.util.HttpUtil;
import com.promoit.otp.model.Role;
import com.promoit.otp.model.User;
import com.promoit.otp.service.AuthService;
import com.promoit.otp.service.LoginResult;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Public authentication endpoints:
 * <pre>
 *   POST /api/auth/register   {login, password, role?}
 *   POST /api/auth/login      {login, password}
 * </pre>
 */
public class AuthHandler extends BaseHandler {

    private final AuthService authService;

    public AuthHandler(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void route(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.endsWith("/register")) {
            register(exchange);
        } else if (path.endsWith("/login")) {
            login(exchange);
        } else {
            throw ApiException.notFound("Unknown auth endpoint: " + path);
        }
    }

    private void register(HttpExchange exchange) throws IOException {
        HttpUtil.requireMethod(exchange, "POST");
        JsonNode body = HttpUtil.readBody(exchange);
        String login = HttpUtil.requiredText(body, "login");
        String password = HttpUtil.requiredText(body, "password");
        Role role = parseRole(HttpUtil.optionalText(body, "role"));

        User user = authService.register(login, password, role);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", user.getId());
        response.put("login", user.getLogin());
        response.put("role", user.getRole().name());
        HttpUtil.sendJson(exchange, 201, response);
    }

    private void login(HttpExchange exchange) throws IOException {
        HttpUtil.requireMethod(exchange, "POST");
        JsonNode body = HttpUtil.readBody(exchange);
        String login = HttpUtil.requiredText(body, "login");
        String password = HttpUtil.requiredText(body, "password");

        LoginResult result = authService.login(login, password);
        HttpUtil.sendJson(exchange, 200, result);
    }

    private Role parseRole(String roleText) {
        if (roleText == null) {
            return Role.USER;
        }
        try {
            return Role.valueOf(roleText.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Invalid role '" + roleText + "'; allowed: ADMIN, USER");
        }
    }
}
