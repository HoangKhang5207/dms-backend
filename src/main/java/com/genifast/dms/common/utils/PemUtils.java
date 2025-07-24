package com.genifast.dms.common.utils;

import java.util.Base64;
import java.util.regex.Pattern;

public class PemUtils {
    public static byte[] parseDerFromPem(String pem, String beginMarker, String endMarker) {
        String base64 = pem
                .replaceAll(Pattern.quote(beginMarker), "")
                .replaceAll(Pattern.quote(endMarker), "")
                .replaceAll("\\s+", "");
        return Base64.getDecoder().decode(base64);
    }
}
