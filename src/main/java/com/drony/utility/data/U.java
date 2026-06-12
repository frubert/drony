package com.drony.utility.data;

import java.math.BigDecimal;
import java.math.MathContext;

public final class U {

    private U() { }

    public static boolean isEmptyOrNull(String string) {
        return string == null || string.trim().isEmpty();
    }

    public static String clean(String string) {
        return string == null ? "" : string.trim();
    }

    public static String trimRemoveNull(String s) {
        return s == null ? "" : s.trim();
    }

    public static Long randomLong(Long max) {
        return (long) (Math.random() * max);
    }

    public static String cleanNameForDCOrder(String name) {
        if (isEmptyOrNull(name)) {
            return "";
        }
        name = name.trim();
        name = name.replaceAll(" +", "_");
        name = name.replace(" ", "_");
        name = name.replace(".", "_");
        name = name.replace(",", "_");
        name = name.replace("+", "_");
        name = name.replace("'", "_");
        name = name.replaceAll("[^a-zA-Z0-9_-]", "");
        return name;
    }

    public static String clearStringForPath(String path) {
        path = path.trim();
        path = path.replaceAll("\\s+", "");
        path = path.replaceAll("[^\\w\\s\\-_]", "");
        return path.toLowerCase();
    }

    public static BigDecimal toBigDecimal(String s) {
        if (isEmptyOrNull(s)) {
            return null;
        }
        try {
            return new BigDecimal(s);
        } catch (Exception e) {
            return null;
        }
    }

    public static BigDecimal toBigDecimal(double value) {
        return new BigDecimal(value, MathContext.DECIMAL64);
    }
}
