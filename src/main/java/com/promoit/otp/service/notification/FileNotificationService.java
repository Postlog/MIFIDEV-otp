package com.promoit.otp.service.notification;

import com.promoit.otp.config.AppConfig;
import com.promoit.otp.model.DeliveryChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Persists generated codes by appending them to a file in the project root.
 * This is the always-available fallback channel and the default.
 */
public class FileNotificationService implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(FileNotificationService.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path filePath;

    public FileNotificationService(AppConfig config) {
        this.filePath = Path.of(config.get("otp.file.path", "otp-codes.txt"));
    }

    @Override
    public DeliveryChannel channel() {
        return DeliveryChannel.FILE;
    }

    @Override
    public boolean requiresRecipient() {
        return false;
    }

    @Override
    public synchronized void send(String recipient, String code, String operationId) {
        String line = String.format("%s | operation=%s | code=%s%n",
                LocalDateTime.now().format(TS),
                operationId == null ? "-" : operationId,
                code);
        try {
            Files.writeString(filePath, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            log.info("OTP code saved to file '{}'", filePath.toAbsolutePath());
        } catch (IOException e) {
            throw new NotificationException("Failed to write OTP code to file " + filePath, e);
        }
    }
}
