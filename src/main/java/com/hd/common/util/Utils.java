package com.hd.common.util;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * 通用工具类。
 * 提供文件操作、日期格式化、UUID生成等通用工具方法。
 *
 * @author john
 * @date 2020-01-27
 */
public class Utils {

    /**
     * 创建文件夹。
     * 如果文件夹不存在则创建，包括所有必要的父文件夹。
     *
     * @param path 文件夹路径
     */
    public static void createFolders(String path) {
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    /**
     * 递归删除文件或文件夹。
     * 如果是文件夹，则递归删除所有子文件和子文件夹。
     *
     * 实现原理：
     * 1. 判断目标是否为文件夹
     * 2. 如果是文件，直接删除
     * 3. 如果是文件夹：
     * - 获取文件夹中的所有文件和子文件夹
     * - 递归调用deleteFile删除每个子项
     * - 最后删除空文件夹本身
     *
     * 注意事项：
     * - 使用递归方式实现，对于深层嵌套的文件夹可能导致栈溢出
     * - 删除操作不可逆，请谨慎使用
     * - 如果文件被占用或权限不足，删除可能失败
     *
     * @param file 要删除的文件或文件夹对象
     */
    public static void deleteFile(File file) {
        // 空值检查，防止空指针异常
        if (file == null || !file.exists()) {
            return;
        }

        // 判断是否为文件夹
        if (!file.isDirectory()) {
            // 如果是文件，直接删除
            if (!file.delete()) {
                // 删除失败时记录日志（静默处理，避免影响调用方）
            }
        } else {
            // 如果是文件夹，先获取所有子项
            File[] files = file.listFiles();

            // listFiles() 可能返回 null，需要进行空值检查
            if (files != null) {
                // 递归删除每个子项
                for (File f : files) {
                    deleteFile(f);
                }
            }

            // 所有子项删除完成后，删除空文件夹本身
            if (!file.delete()) {
                // 删除失败时记录日志（静默处理，避免影响调用方）
            }
        }
    }

    /**
     * 根据路径删除文件或文件夹。
     *
     * @param path 文件或文件夹路径
     */
    public static void deleteFile(String path) {
        deleteFile(new File(path));
    }

    /**
     * 格式化日期。
     * 使用指定的格式字符串格式化日期对象。
     *
     * @param date   要格式化的日期对象
     * @param format 日期格式字符串，如"yyyy-MM-dd"
     * @return 格式化后的日期字符串
     */
    public static String formatDate(Date date, String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        return dateFormat.format(date);
    }

    /**
     * 生成UUID字符串。
     * 生成随机UUID并移除其中的连字符。
     *
     * @return 不包含连字符的UUID字符串
     */
    public static String uuid() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    private Utils() {
    }
}
