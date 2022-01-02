package com.dreamcloud.esa.tools;

public class StringUtils {
    public static boolean nonEmpty(String s) {
        return s != null && !s.equals("");
    }

    public static String normalizeWikiTitle(String title) {
        if (title == null || "".equals(title)) {
            return title;
        }

        char[] str = title.toCharArray();
        for (int ci = 0; ci < str.length; ci++) {
            if (ci == 0) {
                str[ci] = Character.toLowerCase(str[ci]);
            } else if(str[ci - 1] == ':') {
                str[ci] = Character.toLowerCase(str[ci]);
                break;
            }
        }
        title = new String(str);

        return title.replace('_', ' ').trim().replaceAll("[\\s]+", " ");
    }
}
