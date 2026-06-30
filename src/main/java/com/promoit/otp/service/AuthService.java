package com.promoit.otp.service;

import com.promoit.otp.ApiException;
import com.promoit.otp.dao.DuplicateKeyException;
import com.promoit.otp.dao.UserDao;
import com.promoit.otp.model.Role;
import com.promoit.otp.model.User;
import com.promoit.otp.security.PasswordEncoder;
import com.promoit.otp.security.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles registration and login. Enforces the rule that at most one
 * administrator may exist and issues JWT access tokens on login.
 */
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UserDao userDao, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    /**
     * Registers a new user.
     *
     * @param role requested role; when ADMIN, registration fails if an admin already exists
     */
    public User register(String login, String password, Role role) {
        validateCredentials(login, password);

        if (userDao.existsByLogin(login)) {
            throw ApiException.conflict("User with login '" + login + "' already exists");
        }
        if (role == Role.ADMIN && userDao.countByRole(Role.ADMIN) > 0) {
            throw ApiException.conflict("An administrator already exists; a second one cannot be registered");
        }

        User user = new User();
        user.setLogin(login);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        try {
            long id = userDao.insert(user);
            user.setId(id);
        } catch (DuplicateKeyException e) {
            // Lost a race on the unique login / single-admin index.
            throw ApiException.conflict("Registration conflict: login taken or administrator already exists");
        }
        log.info("Registered new user '{}' with role {}", login, role);
        return user;
    }

    /** Authenticates a user and returns a signed access token. */
    public LoginResult login(String login, String password) {
        User user = userDao.findByLogin(login)
                .orElseThrow(() -> ApiException.unauthorized("Invalid login or password"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("Failed login attempt for '{}'", login);
            throw ApiException.unauthorized("Invalid login or password");
        }
        String token = tokenService.generateToken(user.getId(), user.getLogin(), user.getRole());
        log.info("User '{}' logged in (role {})", login, user.getRole());
        return new LoginResult(
                token,
                user.getRole().name(),
                tokenService.getTtlSeconds(),
                tokenService.expiryFromNow().toString());
    }

    private void validateCredentials(String login, String password) {
        if (login == null || login.isBlank() || login.length() > 64) {
            throw ApiException.badRequest("Login must be 1..64 non-blank characters");
        }
        if (password == null || password.length() < 4) {
            throw ApiException.badRequest("Password must be at least 4 characters");
        }
    }
}
