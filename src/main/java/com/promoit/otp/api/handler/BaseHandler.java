package com.promoit.otp.api.handler;

import com.promoit.otp.ApiException;
import com.promoit.otp.api.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Base for all handlers: provides uniform exception-to-HTTP mapping so each
 * concrete handler only implements its routing/business glue in {@link #route}.
 */
public abstract class BaseHandler implements HttpHandler {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public final void handle(HttpExchange exchange) throws IOException {
        try {
            route(exchange);
        } catch (ApiException e) {
            HttpUtil.sendError(exchange, e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Unhandled error in {}: {}", getClass().getSimpleName(), e.getMessage(), e);
            HttpUtil.sendError(exchange, 500, "Internal server error");
        } finally {
            exchange.close();
        }
    }

    protected abstract void route(HttpExchange exchange) throws IOException;
}
