package com.dreamcloud.esa.tools;

import java.util.Locale;

public class StringUtils {
    public static boolean nonEmpty(String s) {
        return s != null && !s.equals("");
    }

    public static String normalizeWikiTitle(String title) {
        return title.toLowerCase(Locale.ROOT).replaceAll("[^a-z ]+", "");
    }
}
