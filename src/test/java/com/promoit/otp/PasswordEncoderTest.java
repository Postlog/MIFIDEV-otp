package com.promoit.otp;

import com.promoit.otp.security.PasswordEncoder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordEncoderTest {

    private final PasswordEncoder encoder = new PasswordEncoder();

    @Test
    void matchesCorrectPassword() {
        String hash = encoder.encode("s3cret!");
        assertTrue(encoder.matches("s3cret!", hash));
    }

    @Test
    void rejectsWrongPassword() {
        String hash = encoder.encode("s3cret!");
        assertFalse(encoder.matches("wrong", hash));
    }

    @Test
    void producesSaltedDistinctHashes() {
        // Same password hashed twice must differ thanks to the random salt.
        assertNotEquals(encoder.encode("same"), encoder.encode("same"));
    }
}
