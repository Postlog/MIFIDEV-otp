package com.promoit.otp.model;

import java.time.LocalDateTime;

/**
 * Domain representation of an application user.
 */
public class User {

    private long id;
    private String login;
    private String passwordHash;
    private Role role;
    private LocalDateTime createdAt;

    public User() {
    }

    public User(long id, String login, String passwordHash, Role role, LocalDateTime createdAt) {
        this.id = id;
        this.login = login;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
