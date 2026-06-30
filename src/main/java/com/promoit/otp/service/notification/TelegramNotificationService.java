package com.promoit.otp.service.notification;

import com.promoit.otp.config.AppConfig;
import com.promoit.otp.model.DeliveryChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Sends OTP codes through the Telegram Bot API. The bot token and a default
 * chat id come from configuration; the caller may override the chat id via the
 * recipient argument.
 */
public class TelegramNotificationService implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationService.class);

    private final boolean enabled;
    private final String apiUrl;
    private final String botToken;
    private final String defaultChatId;
    private final HttpClient httpClient;

    public TelegramNotificationService(AppConfig config) {
        this.enabled = config.getBoolean("telegram.enabled", false);
        this.apiUrl = config.get("telegram.api.url", "https://api.telegram.org");
        this.botToken = config.get("telegram.bot.token", "");
        this.defaultChatId = config.get("telegram.chat.id", "");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public DeliveryChannel channel() {
        return DeliveryChannel.TELEGRAM;
    }

    @Override
    public boolean requiresRecipient() {
        // Falls back to the configured default chat id when not supplied.
        return false;
    }

    @Override
    public void send(String recipient, String code, String operationId) {
        if (!enabled) {
            throw new NotificationException("Telegram channel is disabled");
        }
        if (botToken == null || botToken.isBlank()) {
            throw new NotificationException("Telegram bot token is not configured");
        }
        String chatId = (recipient == null || recipient.isBlank()) ? defaultChatId : recipient;
        if (chatId == null || chatId.isBlank()) {
            throw new NotificationException("Telegram chat id is not configured");
        }

        String text = "Your confirmation code is: " + code
                + (operationId == null ? "" : " (operation " + operationId + ")");
        String url = String.format("%s/bot%s/sendMessage?chat_id=%s&text=%s",
                apiUrl, botToken, urlEncode(chatId), urlEncode(text));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new NotificationException("Telegram API returned status " + response.statusCode()
                        + ": " + response.body());
            }
            log.info("OTP code sent via Telegram to chat {}", chatId);
        } catch (NotificationException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NotificationException("Telegram request was interrupted", e);
        } catch (Exception e) {
            throw new NotificationException("Failed to send Telegram message", e);
        }
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
