package com.promoit.otp.model;

/**
 * Channel through which a generated OTP code is delivered to the user.
 */
public enum DeliveryChannel {
    /** Send via SMTP (Jakarta Mail / Angus Mail). */
    EMAIL,
    /** Send via the SMPP protocol to an emulator such as SMPPsim. */
    SMS,
    /** Send via the Telegram Bot API. */
    TELEGRAM,
    /** Append the code to a file in the project root. */
    FILE
}
