package com.genifast.dms.common.constant;

import java.text.DecimalFormat;

public class FileSizeFormatter {

    private static final String[] UNITS = { "B", "KB", "MB", "GB", "TB" };
    private static final DecimalFormat DF = new DecimalFormat("#,##0.#");

    public static String format(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        double value = bytes / Math.pow(1024, exp);
        return DF.format(value) + " " + UNITS[exp];
    }
}