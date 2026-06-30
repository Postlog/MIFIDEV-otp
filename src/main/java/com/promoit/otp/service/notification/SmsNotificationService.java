package com.promoit.otp.service.notification;

import com.promoit.otp.config.AppConfig;
import com.promoit.otp.model.DeliveryChannel;
import org.jsmpp.bean.Alphabet;
import org.jsmpp.bean.BindType;
import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.GeneralDataCoding;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.RegisteredDelivery;
import org.jsmpp.bean.SMSCDeliveryReceipt;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Sends OTP codes via the SMPP protocol to an emulator such as SMPPsim.
 * Implementation follows the integration guide from the assignment.
 */
public class SmsNotificationService implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationService.class);

    private final boolean enabled;
    private final String host;
    private final int port;
    private final String systemId;
    private final String password;
    private final String systemType;
    private final String sourceAddress;

    public SmsNotificationService(AppConfig config) {
        this.enabled = config.getBoolean("sms.enabled", true);
        this.host = config.get("smpp.host", "localhost");
        this.port = config.getInt("smpp.port", 2775);
        this.systemId = config.get("smpp.system_id", "smppclient1");
        this.password = config.get("smpp.password", "password");
        this.systemType = config.get("smpp.system_type", "OTP");
        this.sourceAddress = config.get("smpp.source_addr", "OTPService");
    }

    @Override
    public DeliveryChannel channel() {
        return DeliveryChannel.SMS;
    }

    @Override
    public boolean requiresRecipient() {
        return true;
    }

    @Override
    public void send(String destination, String code, String operationId) {
        if (!enabled) {
            throw new NotificationException("SMS channel is disabled");
        }
        SMPPSession session = new SMPPSession();
        try {
            BindParameter bindParameter = new BindParameter(
                    BindType.BIND_TX,
                    systemId,
                    password,
                    systemType,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    sourceAddress);

            session.connectAndBind(host, port, bindParameter);

            session.submitShortMessage(
                    systemType,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    sourceAddress,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    destination,
                    new ESMClass(),
                    (byte) 0,
                    (byte) 1,
                    null,
                    null,
                    new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT),
                    (byte) 0,
                    new GeneralDataCoding(Alphabet.ALPHA_DEFAULT),
                    (byte) 0,
                    ("Your code: " + code).getBytes(StandardCharsets.UTF_8));

            log.info("OTP code sent via SMPP to {}", destination);
        } catch (Exception e) {
            throw new NotificationException("Failed to send SMS to " + destination, e);
        } finally {
            session.unbindAndClose();
        }
    }
}
