package com.genifast.dms.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

@Component
public class CommonUtils {

    private static final String VIETNAM_PHONE_PREFIX = "+84";
    private static final Pattern GLOBAL_PHONE_PATTERN = Pattern.compile("^\\+84[1-9]\\d{8}$");
    private static final Pattern LOCAL_PHONE_PATTERN = Pattern.compile("^0[1-9]\\d{8}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+" +
            "@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?" +
            "(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)" +
            "(?=.*[!@#$%^&*()_+}{\":;'?/.,<>]).{8,20}$");

    private static final String CHARSET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CommonUtils() {
        /* utility */ }

    // --- Phone validation & normalization ---

    public static boolean isValidGlobalPhone(String phone) {
        return GLOBAL_PHONE_PATTERN.matcher(phone).matches();
    }

    public static boolean isValidLocalPhone(String phone) {
        return LOCAL_PHONE_PATTERN.matcher(phone).matches();
    }

    /** Chuyển 0xxxxxxxx -> +84xxxxxxxx */
    public static String toGlobalPhone(String phone) {
        if (LOCAL_PHONE_PATTERN.matcher(phone).matches()) {
            return VIETNAM_PHONE_PREFIX + phone.substring(1);
        }
        return phone;
    }

    /** Chuyển +84xxxxxxxx -> 0xxxxxxxx */
    public static String toLocalPhone(String phone) {
        if (GLOBAL_PHONE_PATTERN.matcher(phone).matches()) {
            return "0" + phone.substring(VIETNAM_PHONE_PREFIX.length());
        }
        return phone;
    }

    // --- Email & Password validation ---

    public static boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isEmail(String str) {
        return str != null && str.contains("@");
    }

    public static boolean isValidPassword(String password) {
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    // --- Password encryption & check ---

    public static String encryptPassword(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt(14));
    }

    public static boolean checkPassword(String hashed, String plain) {
        return BCrypt.checkpw(plain, hashed);
    }

    // --- Event encoding: JSON + MD5(payload) -> payloadId ---

    // public static <T> byte[] encodeEvent(Event<T> event) {
    // try {
    // // 1) serialize payload
    // byte[] payloadBytes = MAPPER.writeValueAsBytes(event.getPayload());

    // // 2) MD5 hash for payloadId
    // MessageDigest md5 = MessageDigest.getInstance("MD5");
    // byte[] hash = md5.digest(payloadBytes);
    // String hexId = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    // event.setPayloadId(hexId);

    // // 3) serialize full event
    // return MAPPER.writeValueAsBytes(event);
    // } catch (Exception ex) {
    // throw new IllegalStateException("Failed to encode event", ex);
    // }
    // }

    // --- Random string generator ---

    public static String randomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARSET.charAt(RANDOM.nextInt(CHARSET.length())));
        }
        return sb.toString();
    }
}
