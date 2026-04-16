package com.hd.util;

/**
 * 文件大小格式化工具类。
 * 将字节数转换为易读的格式，支持 B、KB、MB、GB、TB 单位。
 *
 * @author john
 */
public class FileSizeFormatter {

    private static final String[] UNITS = {"B", "KB", "MB", "GB", "TB"};

    /**
     * 将字节数格式化为易读的字符串。
     *
     * @param size 文件大小（字节）
     * @return 格式化后的字符串，如 "1.50 MB"
     */
    public static String format(long size) {
        if (size < 0) {
            return "0 B";
        }
        if (size < 1024) {
            return size + " B";
        }
        int unitIndex = (int) (Math.log10(size) / Math.log10(1024));
        if (unitIndex >= UNITS.length) {
            unitIndex = UNITS.length - 1;
        }
        double formattedSize = size / Math.pow(1024, unitIndex);
        return String.format("%.2f %s", formattedSize, UNITS[unitIndex]);
    }
}
