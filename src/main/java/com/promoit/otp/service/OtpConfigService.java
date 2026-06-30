package com.promoit.otp.service;

import com.promoit.otp.ApiException;
import com.promoit.otp.dao.OtpConfigDao;
import com.promoit.otp.model.OtpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads and updates the single OTP configuration record (code length and TTL).
 */
public class OtpConfigService {

    private static final Logger log = LoggerFactory.getLogger(OtpConfigService.class);

    private static final int MIN_LENGTH = 4;
    private static final int MAX_LENGTH = 10;

    private final OtpConfigDao configDao;

    public OtpConfigService(OtpConfigDao configDao) {
        this.configDao = configDao;
    }

    /** Seeds the single configuration row on first start-up if it is absent. */
    public void ensureSeeded(int defaultLength, long defaultTtlSeconds) {
        configDao.insertIfAbsent(defaultLength, defaultTtlSeconds);
        OtpConfig config = getConfig();
        log.info("OTP configuration ready: length={}, ttl={}s", config.getCodeLength(), config.getTtlSeconds());
    }

    public OtpConfig getConfig() {
        return configDao.find()
                .orElseThrow(() -> new IllegalStateException("OTP configuration row is missing"));
    }

    /** Updates the OTP code length and lifetime after validating the bounds. */
    public OtpConfig updateConfig(int codeLength, long ttlSeconds) {
        if (codeLength < MIN_LENGTH || codeLength > MAX_LENGTH) {
            throw ApiException.badRequest("codeLength must be between " + MIN_LENGTH + " and " + MAX_LENGTH);
        }
        if (ttlSeconds <= 0) {
            throw ApiException.badRequest("ttlSeconds must be positive");
        }
        OtpConfig updated = configDao.update(codeLength, ttlSeconds);
        log.info("OTP configuration updated: length={}, ttl={}s", codeLength, ttlSeconds);
        return updated;
    }
}
