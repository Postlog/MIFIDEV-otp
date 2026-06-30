package com.promoit.otp;

import com.promoit.otp.api.HttpServerBootstrap;
import com.promoit.otp.api.handler.AdminHandler;
import com.promoit.otp.api.handler.AuthHandler;
import com.promoit.otp.api.handler.OtpHandler;
import com.promoit.otp.api.middleware.AuthSupport;
import com.promoit.otp.config.AppConfig;
import com.promoit.otp.config.DatabaseManager;
import com.promoit.otp.dao.OtpCodeDao;
import com.promoit.otp.dao.OtpConfigDao;
import com.promoit.otp.dao.UserDao;
import com.promoit.otp.model.DeliveryChannel;
import com.promoit.otp.scheduler.OtpExpirationScheduler;
import com.promoit.otp.security.PasswordEncoder;
import com.promoit.otp.security.TokenService;
import com.promoit.otp.service.AuthService;
import com.promoit.otp.service.OtpConfigService;
import com.promoit.otp.service.OtpService;
import com.promoit.otp.service.UserService;
import com.promoit.otp.service.notification.NotificationDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application entry point. Wires together the three layers (API → service → DAO),
 * starts the expiration scheduler and the HTTP server, and registers a graceful
 * shutdown hook.
 */
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws Exception {
        AppConfig config = new AppConfig();

        // ---- Infrastructure ----
        DatabaseManager db = new DatabaseManager(config);
        db.initSchema();

        // ---- DAO layer ----
        UserDao userDao = new UserDao(db);
        OtpCodeDao otpCodeDao = new OtpCodeDao(db);
        OtpConfigDao otpConfigDao = new OtpConfigDao(db);

        // ---- Security ----
        PasswordEncoder passwordEncoder = new PasswordEncoder();
        TokenService tokenService = new TokenService(config);

        // ---- Service layer ----
        OtpConfigService configService = new OtpConfigService(otpConfigDao);
        configService.ensureSeeded(
                config.getInt("otp.default.length", 6),
                config.getLong("otp.default.ttl.seconds", 300));

        NotificationDispatcher dispatcher = new NotificationDispatcher(config);
        DeliveryChannel defaultChannel = DeliveryChannel.valueOf(
                config.get("otp.default.channel", "FILE").toUpperCase());

        OtpService otpService = new OtpService(otpCodeDao, configService, dispatcher, defaultChannel);
        AuthService authService = new AuthService(userDao, passwordEncoder, tokenService);
        UserService userService = new UserService(userDao, otpCodeDao);
        AuthSupport authSupport = new AuthSupport(tokenService);

        // ---- Background scheduler ----
        OtpExpirationScheduler scheduler = new OtpExpirationScheduler(
                otpService, config.getLong("otp.scheduler.interval.seconds", 30));
        scheduler.start();

        // ---- API layer ----
        AuthHandler authHandler = new AuthHandler(authService);
        OtpHandler otpHandler = new OtpHandler(otpService, authSupport);
        AdminHandler adminHandler = new AdminHandler(configService, userService, authSupport);

        HttpServerBootstrap http = new HttpServerBootstrap(
                config.getInt("server.port", 8080),
                config.getInt("server.threads", 16),
                authHandler, otpHandler, adminHandler);
        http.start();

        log.info("MIFIDEV-otp service started successfully");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down MIFIDEV-otp service...");
            http.close();
            scheduler.close();
            db.close();
        }));
    }
}
