package com.promoit.otp.api.middleware;

import com.promoit.otp.ApiException;
import com.promoit.otp.security.AuthPrincipal;
import com.promoit.otp.security.TokenService;
import com.sun.net.httpserver.HttpExchange;

/**
 * Token-based authentication and role authorisation for HTTP handlers. Extracts
 * the bearer token, verifies it and enforces the required role, attaching the
 * resolved identity to the exchange so the logging filter can report it.
 */
public class AuthSupport {

    private final TokenService tokenService;

    public AuthSupport(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    /** Requires any authenticated user. */
    public AuthPrincipal requireAuth(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw ApiException.unauthorized("Missing or malformed Authorization header (expected 'Bearer <token>')");
        }
        String token = header.substring("Bearer ".length()).trim();
        try {
            AuthPrincipal principal = tokenService.verify(token);
            exchange.setAttribute("principal", principal.login() + "/" + principal.role());
            return principal;
        } catch (TokenService.InvalidTokenException e) {
            throw ApiException.unauthorized("Invalid or expired token");
        }
    }

    /** Requires an authenticated user with the ADMIN role. */
    public AuthPrincipal requireAdmin(HttpExchange exchange) {
        AuthPrincipal principal = requireAuth(exchange);
        if (!principal.isAdmin()) {
            throw ApiException.forbidden("Administrator role required");
        }
        return principal;
    }
}
