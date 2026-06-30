package com.promoit.otp.service;

/** Summary of a user deletion, including how many OTP codes were removed with them. */
public record DeletionResult(long userId, String login, int deletedOtpCodes) {
}
