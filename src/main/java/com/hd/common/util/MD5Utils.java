package com.hd.common.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd.common.util
 * @createTime 2026/04/23 23:34
 * @description MD5工具类。提供文件MD5校验和计算功能，支持秒传功能。
 *
 * <p>功能特性：
 * <ul>
 *   <li>支持大文件的流式MD5计算，避免内存溢出</li>
 *   <li>提供增量式MD5计算接口</li>
 *   <li>支持进度回调，便于UI展示</li>
 *   <li>高性能的缓冲区读取优化</li>
 * </ul>
 *
 * @author john
 * @date 2020-01-08
 */
@Slf4j
public class MD5Utils {

    /** 默认缓冲区大小（8KB），平衡内存使用和IO性能。 */
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    /** 大文件缓冲区大小（64KB），提高大文件的读取性能。 */
    private static final int LARGE_FILE_BUFFER_SIZE = 65536;

    /** 大文件阈值（100MB），超过此值使用大缓冲区。 */
    private static final long LARGE_FILE_THRESHOLD = 100 * 1024 * 1024;

    /**
     * 计算文件的MD5值。
     * 使用流式读取方式，支持大文件处理，避免内存溢出。
     * 根据文件大小自动选择最优缓冲区大小。
     *
     * @param filePath 文件路径
     * @return MD5十六进制字符串，如果计算失败返回null
     */
    public static String calculateMD5(Path filePath) {
        return calculateMD5(filePath, null);
    }

    /**
     * 计算文件的MD5值（带进度回调）。
     * 使用流式读取方式，支持大文件处理，避免内存溢出。
     *
     * <p>性能优化：
     * <ul>
     *   <li>根据文件大小自动选择缓冲区：小文件8KB，大文件64KB</li>
     *   <li>使用DigestInputStream自动更新摘要</li>
     *   <li>支持进度回调，便于展示上传/计算进度</li>
     * </ul>
     *
     * @param filePath       文件路径
     * @param progressCallback 进度回调接口，可为null
     * @return MD5十六进制字符串，如果计算失败返回null
     */
    public static String calculateMD5(Path filePath, MD5ProgressCallback progressCallback) {
        // 参数校验
        if (filePath == null) {
            log.error("文件路径为空，无法计算MD5");
            return null;
        }

        log.debug("开始计算文件MD5 [filePath={}]", filePath);

        // 检查文件是否存在
        if (!Files.exists(filePath)) {
            log.error("文件不存在 [filePath={}]", filePath);
            return null;
        }

        try {
            // 获取文件大小，用于进度计算和缓冲区选择
            long fileSize = Files.size(filePath);
            int bufferSize = chooseBufferSize(fileSize);

            MessageDigest md = MessageDigest.getInstance("MD5");
            long bytesRead = 0;

            try (InputStream is = Files.newInputStream(filePath);
                    DigestInputStream dis = new DigestInputStream(is, md)) {

                // 创建缓冲区，逐块读取文件
                byte[] buffer = new byte[bufferSize];
                int readCount;
                while ((readCount = dis.read(buffer)) != -1) {
                    bytesRead += readCount;

                    // 回调进度（如果提供了回调接口）
                    if (progressCallback != null && fileSize > 0) {
                        int progress = (int) ((bytesRead * 100) / fileSize);
                        progressCallback.onProgress(progress, bytesRead, fileSize);
                    }
                }
            }

            // 获取MD5摘要
            byte[] digest = md.digest();
            String md5 = bytesToHex(digest);

            log.debug("文件MD5计算完成 [filePath={}, md5={}, fileSize={}, 耗时统计=已读取{}字节]",
                    filePath, md5, fileSize, bytesRead);

            // 回调完成通知
            if (progressCallback != null) {
                progressCallback.onComplete(md5, bytesRead);
            }

            return md5;

        } catch (NoSuchAlgorithmException e) {
            log.error("MD5算法不可用 [filePath={}]", filePath, e);
            return null;
        } catch (IOException e) {
            log.error("读取文件失败 [filePath={}]", filePath, e);
            return null;
        }
    }

    /**
     * 计算输入流的MD5值。
     * 适用于需要从流中计算MD5的场景，如分块上传。
     * 注意：此方法不会关闭输入流，调用者负责关闭。
     *
     * @param inputStream 输入流
     * @return MD5十六进制字符串，如果计算失败返回null
     */
    public static String calculateMD5(InputStream inputStream) {
        return calculateMD5(inputStream, null);
    }

    /**
     * 计算输入流的MD5值（带进度回调）。
     * 适用于需要从流中计算MD5的场景，如分块上传。
     * 注意：此方法不会关闭输入流，调用者负责关闭。
     *
     * @param inputStream      输入流
     * @param progressCallback 进度回调接口，可为null（仅回调已读取字节数）
     * @return MD5十六进制字符串，如果计算失败返回null
     */
    public static String calculateMD5(InputStream inputStream, MD5ProgressCallback progressCallback) {
        // 参数校验
        if (inputStream == null) {
            log.error("输入流为空，无法计算MD5");
            return null;
        }

        log.debug("开始计算输入流MD5");

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            long bytesRead = 0;

            // 注意：不在此处关闭流，由调用者负责关闭
            try (DigestInputStream dis = new DigestInputStream(inputStream, md)) {

                // 创建缓冲区，逐块读取
                byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
                int readCount;
                while ((readCount = dis.read(buffer)) != -1) {
                    bytesRead += readCount;

                    // 回调进度（注意：对于InputStream无法预知总大小，只回传已读取字节）
                    if (progressCallback != null) {
                        progressCallback.onProgress(-1, bytesRead, -1);
                    }
                }
            }

            // 获取MD5摘要
            byte[] digest = md.digest();
            String md5 = bytesToHex(digest);

            log.debug("输入流MD5计算完成 [md5={}, 总读取字节={}]", md5, bytesRead);

            // 回调完成通知
            if (progressCallback != null) {
                progressCallback.onComplete(md5, bytesRead);
            }

            return md5;

        } catch (NoSuchAlgorithmException e) {
            log.error("MD5算法不可用", e);
            return null;
        } catch (IOException e) {
            log.error("读取输入流失败", e);
            return null;
        }
    }

    /**
     * 创建增量式MD5计算器。
     * 适用于分块上传等场景，可以逐步更新MD5值。
     *
     * <p>使用示例：
     * <pre>{@code
     * MD5Calculator calculator = MD5Utils.createIncrementalCalculator();
     * calculator.update(chunk1);
     * calculator.update(chunk2);
     * String md5 = calculator.digest();
     * }</pre>
     *
     * @return 增量式MD5计算器实例
     */
    public static MD5Calculator createIncrementalCalculator() {
        return new MD5Calculator();
    }

    /**
     * 验证文件的MD5值是否匹配。
     *
     * @param filePath    文件路径
     * @param expectedMD5 预期的MD5值
     * @return true表示匹配，false表示不匹配或计算失败
     */
    public static boolean verifyMD5(Path filePath, String expectedMD5) {
        if (expectedMD5 == null || expectedMD5.isEmpty()) {
            log.warn("预期MD5值为空，无法验证 [filePath={}]", filePath);
            return false;
        }

        String actualMD5 = calculateMD5(filePath);
        if (actualMD5 == null) {
            log.error("计算文件MD5失败，无法验证 [filePath={}]", filePath);
            return false;
        }

        boolean match = actualMD5.equalsIgnoreCase(expectedMD5);
        log.debug("MD5验证结果 [filePath={}, expected={}, actual={}, match={}]",
                filePath, expectedMD5, actualMD5, match);

        return match;
    }

    /**
     * 根据文件大小选择最优缓冲区大小。
     * 大文件使用更大的缓冲区以提高IO性能。
     *
     * @param fileSize 文件大小（字节）
     * @return 缓冲区大小（字节）
     */
    private static int chooseBufferSize(long fileSize) {
        if (fileSize > LARGE_FILE_THRESHOLD) {
            log.debug("使用大文件缓冲区 [fileSize={}, bufferSize={}]KB",
                    fileSize, LARGE_FILE_BUFFER_SIZE / 1024);
            return LARGE_FILE_BUFFER_SIZE;
        }
        return DEFAULT_BUFFER_SIZE;
    }

    /**
     * 将字节数组转换为十六进制字符串。
     *
     * @param bytes 字节数组
     * @return 十六进制字符串（小写）
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 私有构造函数，防止实例化。
     */
    private MD5Utils() {
    }

    /**
     * MD5计算进度回调接口。
     * 用于在MD5计算过程中接收进度通知。
     */
    public interface MD5ProgressCallback {

        /**
         * 进度回调方法。
         * 在读取文件数据时周期性调用，用于更新UI进度条。
         *
         * @param progress  进度百分比（0-100），如果总大小未知则为-1
         * @param bytesRead 已读取的字节数
         * @param totalSize 总字节数，如果未知则为-1
         */
        void onProgress(int progress, long bytesRead, long totalSize);

        /**
         * 完成回调方法。
         * 当MD5计算完成时调用。
         *
         * @param md5       计算得到的MD5值
         * @param totalBytes 总共读取的字节数
         */
        default void onComplete(String md5, long totalBytes) {
            // 默认实现为空，子类可以按需重写
        }
    }

    /**
     * 增量式MD5计算器。
     * 支持分块更新MD5值，适用于分块上传等场景。
     *
     * <p>线程安全性：此类不是线程安全的，不应在多线程环境中共享实例。
     */
    public static class MD5Calculator {

        /** MD5摘要算法实例。 */
        private final MessageDigest messageDigest;

        /** 已处理的字节总数。 */
        private long totalBytes = 0;

        /**
         * 创建增量式MD5计算器。
         */
        public MD5Calculator() {
            try {
                this.messageDigest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                // MD5算法在所有JVM实现中都可用，理论上不会抛出此异常
                throw new RuntimeException("MD5算法不可用", e);
            }
        }

        /**
         * 更新MD5计算，传入新的数据块。
         *
         * @param data 数据块
         * @return 当前实例，支持链式调用
         */
        public MD5Calculator update(byte[] data) {
            if (data != null && data.length > 0) {
                messageDigest.update(data);
                totalBytes += data.length;
            }
            return this;
        }

        /**
         * 更新MD5计算，传入数据的指定部分。
         *
         * @param data   数据数组
         * @param offset 起始偏移量
         * @param length 长度
         * @return 当前实例，支持链式调用
         */
        public MD5Calculator update(byte[] data, int offset, int length) {
            if (data != null && length > 0) {
                messageDigest.update(data, offset, length);
                totalBytes += length;
            }
            return this;
        }

        /**
         * 完成MD5计算并返回十六进制字符串。
         * 调用后此计算器不能再使用。
         *
         * @return MD5十六进制字符串
         */
        public String digest() {
            byte[] digest = messageDigest.digest();
            String md5 = bytesToHex(digest);
            log.debug("增量式MD5计算完成 [md5={}, 总处理字节={}]", md5, totalBytes);
            return md5;
        }

        /**
         * 获取已处理的字节总数。
         *
         * @return 已处理的字节数
         */
        public long getTotalBytes() {
            return totalBytes;
        }
    }
}
