package com.promoit.otp.service.notification;

import com.promoit.otp.config.AppConfig;
import com.promoit.otp.model.DeliveryChannel;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Sends OTP codes over SMTP using Jakarta Mail (Angus Mail implementation).
 * Defaults target a MailHog emulator (localhost:1025, no auth), but real SMTP
 * servers work by adjusting the configuration.
 */
public class EmailNotificationService implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final boolean enabled;
    private final String fromEmail;
    private final Session session;

    public EmailNotificationService(AppConfig config) {
        this.enabled = config.getBoolean("email.enabled", true);
        this.fromEmail = config.get("email.from", "otp-service@promoit.local");

        boolean auth = config.getBoolean("email.smtp.auth", false);
        Properties props = new Properties();
        props.put("mail.smtp.host", config.get("email.smtp.host", "localhost"));
        props.put("mail.smtp.port", config.get("email.smtp.port", "1025"));
        props.put("mail.smtp.auth", String.valueOf(auth));
        props.put("mail.smtp.starttls.enable",
                String.valueOf(config.getBoolean("email.smtp.starttls", false)));

        if (auth) {
            String username = config.get("email.username");
            String password = config.get("email.password");
            this.session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
        } else {
            this.session = Session.getInstance(props);
        }
    }

    @Override
    public DeliveryChannel channel() {
        return DeliveryChannel.EMAIL;
    }

    @Override
    public boolean requiresRecipient() {
        return true;
    }

    @Override
    public void send(String recipient, String code, String operationId) {
        if (!enabled) {
            throw new NotificationException("Email channel is disabled");
        }
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            message.setSubject("Your OTP code");
            message.setText("Your verification code is: " + code
                    + (operationId == null ? "" : "\nOperation: " + operationId));
            Transport.send(message);
            log.info("OTP code e-mailed to {}", recipient);
        } catch (MessagingException e) {
            throw new NotificationException("Failed to send email to " + recipient, e);
        }
    }
}
