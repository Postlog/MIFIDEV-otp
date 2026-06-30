package com.promoit.otp.api.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.promoit.otp.ApiException;
import com.promoit.otp.api.middleware.AuthSupport;
import com.promoit.otp.api.util.HttpUtil;
import com.promoit.otp.model.OtpConfig;
import com.promoit.otp.model.User;
import com.promoit.otp.service.DeletionResult;
import com.promoit.otp.service.OtpConfigService;
import com.promoit.otp.service.UserService;
import com.promoit.otp.service.UserView;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.List;

/**
 * Admin-only endpoints (require the ADMIN role):
 * <pre>
 *   GET    /api/admin/config           current OTP configuration
 *   PUT    /api/admin/config           {codeLength, ttlSeconds}
 *   GET    /api/admin/users            list non-admin users
 *   DELETE /api/admin/users/{id}       delete a user and their OTP codes
 * </pre>
 */
public class AdminHandler extends BaseHandler {

    private final OtpConfigService configService;
    private final UserService userService;
    private final AuthSupport auth;

    public AdminHandler(OtpConfigService configService, UserService userService, AuthSupport auth) {
        this.configService = configService;
        this.userService = userService;
        this.auth = auth;
    }

    @Override
    protected void route(HttpExchange exchange) throws IOException {
        // Every admin endpoint is gated by the ADMIN role.
        auth.requireAdmin(exchange);

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if (path.endsWith("/config")) {
            if ("GET".equalsIgnoreCase(method)) {
                getConfig(exchange);
            } else if ("PUT".equalsIgnoreCase(method)) {
                updateConfig(exchange);
            } else {
                throw new ApiException(405, "Method not allowed for /config");
            }
        } else if (path.endsWith("/users")) {
            HttpUtil.requireMethod(exchange, "GET");
            listUsers(exchange);
        } else if (path.matches(".*/users/\\d+")) {
            HttpUtil.requireMethod(exchange, "DELETE");
            deleteUser(exchange, path);
        } else {
            throw ApiException.notFound("Unknown admin endpoint: " + path);
        }
    }

    private void getConfig(HttpExchange exchange) throws IOException {
        OtpConfig config = configService.getConfig();
        HttpUtil.sendJson(exchange, 200, config);
    }

    private void updateConfig(HttpExchange exchange) throws IOException {
        JsonNode body = HttpUtil.readBody(exchange);
        int codeLength = HttpUtil.requiredInt(body, "codeLength");
        long ttlSeconds = HttpUtil.requiredLong(body, "ttlSeconds");
        OtpConfig updated = configService.updateConfig(codeLength, ttlSeconds);
        HttpUtil.sendJson(exchange, 200, updated);
    }

    private void listUsers(HttpExchange exchange) throws IOException {
        List<UserView> users = userService.listNonAdmins().stream()
                .map(this::toView)
                .toList();
        HttpUtil.sendJson(exchange, 200, users);
    }

    private void deleteUser(HttpExchange exchange, String path) throws IOException {
        long id = Long.parseLong(path.substring(path.lastIndexOf('/') + 1));
        DeletionResult result = userService.deleteUser(id);
        HttpUtil.sendJson(exchange, 200, result);
    }

    private UserView toView(User user) {
        return new UserView(
                user.getId(),
                user.getLogin(),
                user.getRole().name(),
                user.getCreatedAt() == null ? null : user.getCreatedAt().toString());
    }
}
