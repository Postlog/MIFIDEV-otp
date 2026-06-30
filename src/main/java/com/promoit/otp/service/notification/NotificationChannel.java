package com.promoit.otp.service.notification;

import com.promoit.otp.model.DeliveryChannel;

/**
 * A delivery mechanism that transmits an OTP code to the user. Each
 * implementation handles exactly one {@link DeliveryChannel}.
 */
public interface NotificationChannel {

    /** Which channel this implementation serves. */
    DeliveryChannel channel();

    /** Whether a recipient address (email/phone) must be supplied by the caller. */
    boolean requiresRecipient();

    /**
     * Delivers the code.
     *
     * @param recipient   channel-specific destination (email, phone, chat id), may be null
     * @param code        the OTP code value
     * @param operationId the operation the code protects, may be null
     * @throws NotificationException if delivery fails
     */
    void send(String recipient, String code, String operationId);
}
