package com.promoit.otp.model;

import java.time.LocalDateTime;

/**
 * The single OTP configuration record: how long a code lives and how
 * many digits it contains.
 */
public class OtpConfig {

    private int id;
    private int codeLength;
    private long ttlSeconds;
    private LocalDateTime updatedAt;

    public OtpConfig() {
    }

    public OtpConfig(int id, int codeLength, long ttlSeconds, LocalDateTime updatedAt) {
        this.id = id;
        this.codeLength = codeLength;
        this.ttlSeconds = ttlSeconds;
        this.updatedAt = updatedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCodeLength() {
        return codeLength;
    }

    public void setCodeLength(int codeLength) {
        this.codeLength = codeLength;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
