package com.genifast.dms.common.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ConvertUtil {
    /**
     * Convert comma-separated string to List<Long>
     *
     * @param commaSeparatedIds e.g. "1, 2, 3 ,4"
     * @return List<Long> e.g. [1, 2, 3, 4]
     */
    public static List<Long> stringToLongList(String commaSeparatedIds) {
        if (commaSeparatedIds == null || commaSeparatedIds.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(commaSeparatedIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    /**
     * Convert comma-separated string to List<Long>
     *
     * @param commaSeparatedIds e.g. "01,02,03"
     * @return List<Long> e.g. ["01", "02", "03"]
     */
    public static List<String> stringToStringList(String commaSeparatedIds) {
        if (commaSeparatedIds == null || commaSeparatedIds.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(commaSeparatedIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public static String longListToString(List<Long> list) {
        if (list == null || list.isEmpty())
            return "";
        return list.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    public static String stringListToString(List<String> list) {
        if (list == null || list.isEmpty())
            return "";
        return String.join(",", list);
    }
}
