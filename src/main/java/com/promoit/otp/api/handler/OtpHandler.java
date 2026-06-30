package com.promoit.otp.api.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.promoit.otp.ApiException;
import com.promoit.otp.api.middleware.AuthSupport;
import com.promoit.otp.api.util.HttpUtil;
import com.promoit.otp.model.DeliveryChannel;
import com.promoit.otp.security.AuthPrincipal;
import com.promoit.otp.service.GenerateResult;
import com.promoit.otp.service.OtpService;
import com.promoit.otp.service.ValidateResult;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

/**
 * Authenticated user endpoints (any role):
 * <pre>
 *   POST /api/otp/generate  {operationId?, channel?, recipient?}
 *   POST /api/otp/validate  {code, operationId?}
 * </pre>
 */
public class OtpHandler extends BaseHandler {

    private final OtpService otpService;
    private final AuthSupport auth;

    public OtpHandler(OtpService otpService, AuthSupport auth) {
        this.otpService = otpService;
        this.auth = auth;
    }

    @Override
    protected void route(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.endsWith("/generate")) {
            generate(exchange);
        } else if (path.endsWith("/validate")) {
            validate(exchange);
        } else {
            throw ApiException.notFound("Unknown OTP endpoint: " + path);
        }
    }

    private void generate(HttpExchange exchange) throws IOException {
        HttpUtil.requireMethod(exchange, "POST");
        AuthPrincipal principal = auth.requireAuth(exchange);

        JsonNode body = HttpUtil.readBody(exchange);
        String operationId = HttpUtil.optionalText(body, "operationId");
        String recipient = HttpUtil.optionalText(body, "recipient");
        DeliveryChannel channel = parseChannel(HttpUtil.optionalText(body, "channel"));

        GenerateResult result = otpService.generate(principal.userId(), operationId, channel, recipient);
        HttpUtil.sendJson(exchange, 201, result);
    }

    private void validate(HttpExchange exchange) throws IOException {
        HttpUtil.requireMethod(exchange, "POST");
        AuthPrincipal principal = auth.requireAuth(exchange);

        JsonNode body = HttpUtil.readBody(exchange);
        String code = HttpUtil.requiredText(body, "code");
        String operationId = HttpUtil.optionalText(body, "operationId");

        ValidateResult result = otpService.validate(principal.userId(), code, operationId);
        HttpUtil.sendJson(exchange, 200, result);
    }

    private DeliveryChannel parseChannel(String channelText) {
        if (channelText == null) {
            return null; // service falls back to the configured default channel
        }
        try {
            return DeliveryChannel.valueOf(channelText.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Invalid channel '" + channelText
                    + "'; allowed: EMAIL, SMS, TELEGRAM, FILE");
        }
    }
}
