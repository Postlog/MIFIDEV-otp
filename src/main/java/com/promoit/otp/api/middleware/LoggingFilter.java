package com.promoit.otp.api.middleware;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * A {@link Filter} that logs every HTTP request on the way in and the matching
 * response (status, authenticated user, duration) on the way out.
 */
public class LoggingFilter extends Filter {

    private static final Logger log = LoggerFactory.getLogger("com.promoit.otp.api.access");

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        long startNanos = System.nanoTime();
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String remote = exchange.getRemoteAddress() == null
                ? "unknown"
                : exchange.getRemoteAddress().getAddress().getHostAddress();

        log.info("--> {} {} from {}", method, path, remote);
        try {
            chain.doFilter(exchange);
        } finally {
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
            Object principal = exchange.getAttribute("principal");
            int status = exchange.getResponseCode();
            log.info("<-- {} {} status={} user={} {}ms",
                    method, path, status, principal == null ? "anonymous" : principal, elapsedMs);
        }
    }

    @Override
    public String description() {
        return "Logs every HTTP request and response";
    }
}
