package com.promoit.otp.service;

import com.promoit.otp.ApiException;
import com.promoit.otp.dao.OtpCodeDao;
import com.promoit.otp.model.DeliveryChannel;
import com.promoit.otp.model.OtpCode;
import com.promoit.otp.model.OtpConfig;
import com.promoit.otp.model.OtpStatus;
import com.promoit.otp.service.notification.NotificationDispatcher;
import com.promoit.otp.service.notification.NotificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;

/**
 * Core OTP business logic: generating codes (and dispatching them through a
 * channel), validating codes and bulk-expiring outdated ones.
 */
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    private final OtpCodeDao otpCodeDao;
    private final OtpConfigService configService;
    private final NotificationDispatcher dispatcher;
    private final DeliveryChannel defaultChannel;
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpCodeDao otpCodeDao,
                      OtpConfigService configService,
                      NotificationDispatcher dispatcher,
                      DeliveryChannel defaultChannel) {
        this.otpCodeDao = otpCodeDao;
        this.configService = configService;
        this.dispatcher = dispatcher;
        this.defaultChannel = defaultChannel;
    }

    /**
     * Generates a fresh OTP for the user, persists it as ACTIVE and dispatches it
     * through the chosen channel. The code value is never returned in the API
     * response; it reaches the user only through the delivery channel.
     */
    public GenerateResult generate(long userId, String operationId, DeliveryChannel channel, String recipient) {
        DeliveryChannel resolved = (channel != null) ? channel : defaultChannel;

        // Validate up-front so a missing recipient fails before we persist anything.
        dispatcher.validateRecipient(resolved, recipient);

        OtpConfig config = configService.getConfig();
        String code = generateNumericCode(config.getCodeLength());

        OtpCode otp = new OtpCode();
        otp.setUserId(userId);
        otp.setOperationId(operationId);
        otp.setCode(code);
        otp.setStatus(OtpStatus.ACTIVE);
        otp.setChannel(resolved);
        // created_at and expires_at are computed by the database (now() + ttl)
        // and read back onto the model — no JVM/DB clock mismatch is possible.
        long id = otpCodeDao.insert(otp, config.getTtlSeconds());

        boolean delivered = true;
        String deliveryError = null;
        try {
            dispatcher.dispatch(resolved, recipient, code, operationId);
        } catch (NotificationException e) {
            delivered = false;
            deliveryError = e.getMessage();
            log.warn("OTP id={} persisted but delivery via {} failed: {}", id, resolved, e.getMessage());
        }

        log.info("Generated OTP id={} for user={} operation={} channel={} delivered={}",
                id, userId, operationId, resolved, delivered);
        return new GenerateResult(id, operationId, OtpStatus.ACTIVE.name(), resolved.name(),
                otp.getExpiresAt().toString(), delivered, deliveryError);
    }

    /**
     * Validates a code submitted by the user. Consumption is atomic: a single
     * conditional UPDATE both checks that the code is ACTIVE and unexpired and
     * marks it USED, so a code can never be redeemed twice (even concurrently).
     */
    public ValidateResult validate(long userId, String code, String operationId) {
        if (code == null || code.isBlank()) {
            throw ApiException.badRequest("'code' is required");
        }

        long consumedId = otpCodeDao.consumeActive(userId, code.trim(), operationId)
                .orElseThrow(() -> ApiException.badRequest("Code is invalid, already used or expired"));

        log.info("OTP id={} validated and marked USED (user={})", consumedId, userId);
        return new ValidateResult(true, OtpStatus.USED.name(), "Code accepted");
    }

    /** Bulk-transitions outdated ACTIVE codes to EXPIRED. Returns how many changed. */
    public int expireOutdated() {
        return otpCodeDao.markExpired();
    }

    private String generateNumericCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
