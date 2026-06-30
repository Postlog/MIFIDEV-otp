package com.promoit.otp.service;

/** Safe projection of a user for API responses (never exposes the password hash). */
public record UserView(long id, String login, String role, String createdAt) {
}
