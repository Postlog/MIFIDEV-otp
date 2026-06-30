package com.promoit.otp.service.notification;

/**
 * Raised when a notification channel cannot deliver a code. The OTP itself is
 * already persisted, so this failure is reported back to the caller without
 * invalidating the generated code.
 */
public class NotificationException extends RuntimeException {

    public NotificationException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotificationException(String message) {
        super(message);
    }
}
