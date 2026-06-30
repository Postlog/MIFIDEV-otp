package com.promoit.otp.scheduler;

import com.promoit.otp.service.OtpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Background job that periodically flips ACTIVE codes whose TTL has elapsed to
 * the EXPIRED status.
 */
public class OtpExpirationScheduler implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(OtpExpirationScheduler.class);

    private final OtpService otpService;
    private final long intervalSeconds;
    private final ScheduledExecutorService executor;

    public OtpExpirationScheduler(OtpService otpService, long intervalSeconds) {
        this.otpService = otpService;
        this.intervalSeconds = intervalSeconds;
        ThreadFactory factory = runnable -> {
            Thread t = new Thread(runnable, "otp-expiration-scheduler");
            t.setDaemon(true);
            return t;
        };
        this.executor = Executors.newSingleThreadScheduledExecutor(factory);
    }

    public void start() {
        executor.scheduleAtFixedRate(this::run, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        log.info("OTP expiration scheduler started (every {}s)", intervalSeconds);
    }

    private void run() {
        try {
            int expired = otpService.expireOutdated();
            if (expired > 0) {
                log.info("Expiration sweep: {} code(s) marked EXPIRED", expired);
            } else {
                log.debug("Expiration sweep: nothing to expire");
            }
        } catch (Exception e) {
            // Never let a sweep failure kill the scheduler thread.
            log.error("Expiration sweep failed: {}", e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
        log.info("OTP expiration scheduler stopped");
    }
}
