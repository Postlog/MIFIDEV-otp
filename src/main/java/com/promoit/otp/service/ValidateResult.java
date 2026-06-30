package com.promoit.otp.service;

/** Outcome of validating an OTP code. */
public record ValidateResult(boolean valid, String status, String message) {
}
