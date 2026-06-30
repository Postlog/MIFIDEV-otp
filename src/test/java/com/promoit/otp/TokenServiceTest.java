package com.promoit.otp;

import com.promoit.otp.config.AppConfig;
import com.promoit.otp.model.Role;
import com.promoit.otp.security.AuthPrincipal;
import com.promoit.otp.security.TokenService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenServiceTest {

    private final TokenService tokenService = new TokenService(new AppConfig());

    @Test
    void generatesAndVerifiesToken() {
        String token = tokenService.generateToken(42L, "alice", Role.ADMIN);
        AuthPrincipal principal = tokenService.verify(token);

        assertEquals(42L, principal.userId());
        assertEquals("alice", principal.login());
        assertEquals(Role.ADMIN, principal.role());
        assertTrue(principal.isAdmin());
    }

    @Test
    void rejectsTamperedToken() {
        String token = tokenService.generateToken(1L, "bob", Role.USER);
        String tampered = token.substring(0, token.length() - 2) + "xx";
        assertThrows(TokenService.InvalidTokenException.class, () -> tokenService.verify(tampered));
    }
}
