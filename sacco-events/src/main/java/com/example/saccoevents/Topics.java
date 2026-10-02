package com.example.saccoevents;

public final class Topics {

    private Topics() {}

    // ---------- Member domain ----------
    public static final String MEMBER_CREATED   = "member.created";
    public static final String MEMBER_UPDATED   = "member.updated";
    public static final String MEMBER_SUSPENDED = "member.suspended";
    public static final String MEMBER_DELETED   = "member.deleted";

    // ---------- Account domain ----------
    public static final String ACCOUNT_CREATED  = "account.created";
    public static final String ACCOUNT_CREDITED = "account.credited";
    public static final String ACCOUNT_DEBITED  = "account.debited";
    public static final String ACCOUNT_FROZEN   = "account.frozen";

    // ---------- Loan domain ----------
    public static final String LOAN_APPLICATION_SUBMITTED = "loan.application.submitted";
    public static final String LOAN_APPLICATION_APPROVED  = "loan.application.approved";
    public static final String LOAN_APPLICATION_REJECTED  = "loan.application.rejected";
    public static final String LOAN_DISBURSED             = "loan.disbursed";
    public static final String LOAN_REPAID                = "loan.repaid";
    // ---------- Auth domain ----------// ---------- Auth domain ----------
    public static final String USER_OTP_REQUESTED = "user.otp.requested";
    public static final String USER_ACTIVATED     = "user.activated";
    // ---------- Notification domain ----------
    public static final String NOTIFICATION_REQUESTED = "notification.requested";
}