package com.dreamcloud.esa.tools;

public class StringUtils {
    public static boolean nonEmpty(String s) {
        return s != null && !s.equals("");
    }

    public static String normalizeWikiTitle(String title) {
        if (title == null || "".equals(title)) {
            return title;
        }

        char[] c = title.toCharArray();
        c[0] = Character.toLowerCase(c[0]);
        title = new String(c);

        return title.replace('_', ' ').trim().replaceAll("[\\s]+", " ");
    }
}
