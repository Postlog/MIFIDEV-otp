package com.promoit.otp.service;

/** Result of a successful login: the access token and its metadata. */
public record LoginResult(String token, String role, long expiresInSeconds, String expiresAt) {
}
