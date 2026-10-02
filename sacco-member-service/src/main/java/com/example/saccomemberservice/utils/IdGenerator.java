package com.example.saccomemberservice.utils;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class IdGenerator {


    private static final String ACCOUNT_PREFIX = "ACCT";
    private static final String MEMBER_PREFIX = "MNO";

    public static UUID generateAccountId() {
        return UUID.randomUUID();
    }

    public static String generateMemberNo() {
        int randomNumber = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return MEMBER_PREFIX + randomNumber;
    }
}
