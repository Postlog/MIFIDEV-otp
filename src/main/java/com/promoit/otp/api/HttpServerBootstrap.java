package com.promoit.otp.api;

import com.promoit.otp.api.handler.AdminHandler;
import com.promoit.otp.api.handler.AuthHandler;
import com.promoit.otp.api.handler.OtpHandler;
import com.promoit.otp.api.middleware.LoggingFilter;
import com.promoit.otp.api.util.HttpUtil;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Builds and runs the {@link HttpServer} (from {@code com.sun.net.httpserver}),
 * wiring each route to its handler and attaching the request-logging filter.
 */
public class HttpServerBootstrap implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(HttpServerBootstrap.class);

    private final HttpServer server;
    private final ExecutorService executor;
    private final int port;

    public HttpServerBootstrap(int port,
                               int threads,
                               AuthHandler authHandler,
                               OtpHandler otpHandler,
                               AdminHandler adminHandler) throws IOException {
        this.port = port;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.executor = Executors.newFixedThreadPool(threads);

        LoggingFilter loggingFilter = new LoggingFilter();
        register("/api/auth", authHandler, loggingFilter);
        register("/api/otp", otpHandler, loggingFilter);
        register("/api/admin", adminHandler, loggingFilter);
        register("/health", healthHandler(), loggingFilter);

        server.setExecutor(executor);
    }

    private void register(String path, HttpHandler handler, LoggingFilter filter) {
        HttpContext context = server.createContext(path, handler);
        context.getFilters().add(filter);
    }

    private HttpHandler healthHandler() {
        return exchange -> {
            try {
                HttpUtil.sendJson(exchange, 200, Map.of("status", "UP"));
            } finally {
                exchange.close();
            }
        };
    }

    public void start() {
        server.start();
        log.info("HTTP server listening on http://localhost:{}", port);
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
        log.info("HTTP server stopped");
    }
}
