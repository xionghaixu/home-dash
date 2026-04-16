package com.hd.util;

/**
 * 字符串工具类。
 * 提供字符串判空、长度检查等常用字符串操作方法。
 *
 * @author john
 * @date 2020-01-11
 */
public class StringUtils {

    /**
     * 空字符串常量。
     */
    public static final String EMPTY = "";

    /**
     * 检查字符串是否有长度。
     * 字符串不为null且长度大于0时返回true。
     *
     * @param str 要检查的字符串
     * @return true表示字符串有长度，false表示字符串为null或空
     */
    public static boolean hasLength(String str) {
        return str != null && !str.isEmpty();
    }

    /**
     * 检查字符串是否为空白。
     * 字符串为null、空字符串或仅包含空白字符时返回true。
     *
     * @param str 要检查的字符串
     * @return true表示字符串为空白，false表示字符串包含非空白字符
     */
    public static boolean isBlank(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if ((!Character.isWhitespace(str.charAt(i)))) {
                return false;
            }
        }
        return true;
    }

    private StringUtils() {
    }
}
