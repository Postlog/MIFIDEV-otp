package com.promoit.otp.service.notification;

import com.promoit.otp.ApiException;
import com.promoit.otp.config.AppConfig;
import com.promoit.otp.model.DeliveryChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;

/**
 * Routes a code to the requested {@link DeliveryChannel}. Owns one instance of
 * every channel implementation and validates that a recipient is present when
 * the channel needs one.
 */
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final Map<DeliveryChannel, NotificationChannel> channels = new EnumMap<>(DeliveryChannel.class);

    public NotificationDispatcher(AppConfig config) {
        register(new EmailNotificationService(config));
        register(new SmsNotificationService(config));
        register(new TelegramNotificationService(config));
        register(new FileNotificationService(config));
    }

    private void register(NotificationChannel channel) {
        channels.put(channel.channel(), channel);
    }

    /**
     * Validates the recipient for the chosen channel. Throws an
     * {@link ApiException} (HTTP 400) when a required recipient is missing.
     */
    public void validateRecipient(DeliveryChannel channel, String recipient) {
        NotificationChannel impl = channels.get(channel);
        if (impl == null) {
            throw ApiException.badRequest("Unsupported delivery channel: " + channel);
        }
        if (impl.requiresRecipient() && (recipient == null || recipient.isBlank())) {
            throw ApiException.badRequest("Channel " + channel + " requires a 'recipient'");
        }
    }

    /**
     * Delivers the code through the chosen channel.
     *
     * @throws NotificationException if delivery fails
     */
    public void dispatch(DeliveryChannel channel, String recipient, String code, String operationId) {
        NotificationChannel impl = channels.get(channel);
        if (impl == null) {
            throw new NotificationException("Unsupported delivery channel: " + channel);
        }
        log.info("Dispatching OTP via {}", channel);
        impl.send(recipient, code, operationId);
    }
}
