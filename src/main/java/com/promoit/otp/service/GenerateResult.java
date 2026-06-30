package com.promoit.otp.service;

/** Outcome of generating and dispatching an OTP code (the code value itself is never returned). */
public record GenerateResult(
        long otpId,
        String operationId,
        String status,
        String channel,
        String expiresAt,
        boolean delivered,
        String deliveryError) {
}
