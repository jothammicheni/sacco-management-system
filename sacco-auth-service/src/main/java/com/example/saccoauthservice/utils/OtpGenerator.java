package com.example.saccoauthservice.utils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

public class OtpGenerator {
    public static void main(String[] args) {
        System.out.println("Generated OTP: " + generateOtp());
    }

    public static String generateOtp() {
        SecureRandom secureRandom = new SecureRandom();
        // Generate a random number from 0 to 9999
        int number = secureRandom.nextInt(10000);

        // %04d pads the number with leading zeros if it is less than 4 digits
        return String.format("%06d", number);
    }

    public static Instant validTill(){
     return   Instant.now().plus(Duration.ofMinutes(10));
    }
}
