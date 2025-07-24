package com.genifast.dms.common.constant;

public enum OtpFactor {
    SMS_OTP_FACTOR("SMS_OTP_FACTOR"),
    EMAIL_OTP_FACTOR("EMAIL_OTP_FACTOR");

    private final String value;

    OtpFactor(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
