package com.genifast.dms.common.constant;

import java.util.Set;

public final class CommonConstants {
    private CommonConstants() {
        /* no-op */ }

    // -- HTTP/Auth headers & tokens --
    public static final String AUTHORIZATION = "Authorization";
    public static final String TOKEN_TYPE_BEARER = "Bearer";
    public static final String TOKEN_TYPE_BASIC = "Basic";

    // -- JWT / OTP types --
    public static final String JWT_OTP_PHONE = "sms";
    public static final String JWT_OTP_EMAIL = "email";
    public static final String JWT_SECURITY_QUESTION = "security_question";
    public static final String JWT_FACE_MATCHING = "face_matching";
    public static final String JWT_TOKEN_LEVEL2 = "token_level2";
    public static final String TOKEN_SOURCE = "token_source";
    public static final String TOKEN_TYPE_HEADER = "X-token-type";

    // -- Keys & attributes --
    public static final String KEY_OPS_MODULE = "ops_module";
    public static final String KEY_OPS_MODULE_CODE = "ops_module_code";
    public static final String ATTRIBUTE_AUDIENCE = "audience";
    public static final String ORIGINAL_SOURCE = "original";
    public static final String SOURCE_ONE_U = "one_u";
    public static final String AUTHEN_SDK_SOURCE = "authen_sdk";
    public static final String ERROR_RAW = "error_raw";
    public static final String CHANNEL = "X-Channel";

    // -- Type & content constants --
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_JSON = "application/json";

    // -- SMS & Email channels/types --
    public static final String SMS_CONNECTION = "sms";
    public static final String SMS_TYPE = "sms";
    public static final String EMAIL_CONNECTION = "email";
    public static final String EMAIL_TYPE = "email";
    public static final String EMAIL = "email";

    // -- OTP verification actions --
    public static final String VERIFY_OTP_TYPE = "Verify_Type";
    public static final String VERIFY_OTP_RESET_PASSWORD = "reset-password";
    public static final String VERIFY_OTP_RESEND_PASSWORD = "resend-password";

    // -- Identifier / subject fields --
    public static final String SUB = "sub";
    public static final String USER_ID = "user_id";
    public static final String TENANT = "tenant";

    // -- Phone/Email types --
    public static final String TYPE_PHONE_NUMBER = "phone_number";
    public static final String TYPE_EMAIL = "email";

    // -- Password/PIN/OTP fields --
    public static final String PWD = "pwd";
    public static final String PIN = "PIN";
    public static final String PIN_LOWER = "pin";
    public static final String PASSWORD_LENGTH_FIELD = "password";
    public static final String OTP_FIELD = "otp";

    // -- Misc strings --
    public static final String SENDER = "sender";
    public static final String SENDER_NAME = "sender_name";
    public static final String AUTHORIZATION_CODE = "authorization_code";
    public static final String ENV = "env";
    public static final String LINK = "link1";
    public static final String EMAIL_NEW = "email_new";
    public static final String EVENT_TYPE_SIGNIN = "signin";
    public static final String EVENT_TYPE_SIGNUP = "signup";

    public static final String TYPE_LOGIN = "login";
    public static final String TYPE_RESET_PASSWORD = "reset_password";
    public static final String TYPE_UNLOCK_PHONE = "phone_number";
    public static final String TYPE_UNLOCK_EMAIL = "email";
    public static final String UNLOCK_OTP_BY_EMAIL = "email";
    public static final String UNLOCK_OTP_BY_PHONE = "phone_number";

    public static final String USER_PASSWORD_CONNECTION = "Username-Password-Authentication";
    public static final String USER_PASSWORD_CONNECTION_VINID = "vinid-auth";
    public static final String LINK_ACCOUNT_BY_EMAIL = "by_email";

    // -- Headers --
    public static final String X_USER_ID = "X-USER-ID";
    public static final String V_USER_ID = "V-USER-ID";
    public static final String REQUEST_ID_KEY = "X-REQUEST-ID";
    public static final String DEVICE_ID_KEY = "X-DEVICE-ID";
    public static final String RF_DEVICE_ID_KEY = "X-RF-Device-ID";
    public static final String REQUEST_DEVICE_ID_KEY = "X-Device-Request-ID";
    public static final String DEVICE_SESSION_ID_KEY = "X-Device-Session-ID";
    public static final String DEVICE_OS_TOKEN = "X-Device-OS-Token";
    public static final String VERSION_HEADER = "X-Version";
    public static final String DEVICE_OS_KEY = "X-Device-OS";
    public static final String DEVICE_VERSION_KEY = "X-Device-Version";
    public static final String DEVICE_UUID = "X-Device-UUID";

    public static final String X_API_KEY = "X-API-KEY";
    public static final String X_API_SECRET = "X-API-SECRET";
    public static final String VINPAY_API_KEY = "X-Api-Key";
    public static final String VINPAY_API_SECRET = "X-Api-Secret";
    public static final String ACCEPT_LANGUAGE = "Accept-Language";
    public static final String VIETNAMESE_LANG = "vi";
    public static final String ENGLISH_LANG = "en";

    // -- Patterns & durations --
    public static final String REGEX_OTP = "^[0-9]{6}.*";
    public static final int NUMBER_SECOND_PER_DAY = 86400;
    public static final int TEST_PHONE_EXPIRE = 120;
    public static final int OTP_TIME_TO_LIVE = 120;
    public static final int PASSWORD_TTL_BY_DAY = 180;
    public static final int PASSWORD_LENGTH = 6;
    public static final int SECURE_PASSWORD_LENGTH = 6;
    public static final int RANDOM_SALT_LENGTH = 36;

    // -- Status constants --
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_INACTIVE = "inactive";
    public static final String STATUS_WAITING_DELETE = "waiting_to_delete";
    public static final String STATUS_CANCEL_DELETE = "cancel_delete";
    public static final String STATUS_DELETED = "deleted";

    public static final Set<String> USER_STATUS_SET = Set.of(
            STATUS_ACTIVE,
            STATUS_INACTIVE,
            STATUS_WAITING_DELETE,
            STATUS_CANCEL_DELETE,
            STATUS_DELETED);

    // -- Misc others --
    public static final String USER_MOBILE_TYPE = "MOBILE";
    public static final long LIMIT_DATA = 10_737_418_240L;

    // -- Template strings --
    public static final String APPLICATION_CONFIG_PATTERN = "oneid:application:config:%s:%s";

    // -- Search & template fields --
    public static final String SEARCH_FIELD_USERNAME = "username";
    public static final String SEARCH_FIELD_PHONE_NUMBER = "user_metadata.phone_number";
    public static final String SEARCH_FIELD_EMAIL = "user_metadata.email";
    public static final String EMAIL_ERROR_TMPL = "email-error.tmpl";
    public static final String DEVICE_TOKEN_GEN_FAILURE = "DEVICE_TOKEN_GENERATION_FAILED";
    public static final String FULLNAME_KH = "Khách Hàng";
}
