package com.promoit.otp.security;

import com.promoit.otp.model.Role;

/**
 * The authenticated identity extracted from a valid JWT, attached to each
 * authorised HTTP exchange.
 */
public record AuthPrincipal(long userId, String login, Role role) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
