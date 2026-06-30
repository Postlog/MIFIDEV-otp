package com.promoit.otp.model;

/**
 * Lifecycle status of an OTP code.
 */
public enum OtpStatus {
    /** Code is active and can be validated. */
    ACTIVE,
    /** Code lived past its TTL and can no longer be used. */
    EXPIRED,
    /** Code was successfully validated and consumed. */
    USED
}
