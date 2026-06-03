package com.hd.common.util;

import com.hd.common.enums.FileTypeEnum;
import com.hd.common.exception.DataFormatException;
import com.hd.common.exception.InvalidFilePathException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd.common.util
 * @createTime 2026/04/23 23:34
 * @description 文件工具类。提供文件类型判断、文件名验证、扩展名提取等文件相关操作方法。
 *
 * <p><b>文件类型分类：</b>
 * <ul>
 *   <li>VIDEO - 视频文件：mp4, flv, rmvb, avi, mkv, mov, wmv, webm, m4v, 3gp</li>
 *   <li>AUDIO - 音频文件：mp3, wav, flac, aac, ogg, wma, m4a</li>
 *   <li>PICTURE - 图片文件：png, jpg, jpeg, gif, ico, bmp, webp, svg, tiff, webp</li>
 *   <li>DOC - 文档文件：doc, docx, xls, xlsx, pages, numbers, key</li>
 *   <li>PDF - PDF文档</li>
 *   <li>TXT - 纯文本文件：txt, md, json, xml, yaml, yml</li>
 *   <li>PPT - 演示文稿：ppt, pptx, key</li>
 *   <li>CODE - 代码文件：java, js, ts, py, go, vue, c, cpp, h, cs, rb, php, swift, kt等</li>
 *   <li>WEB - 网页文件：html, htm, css</li>
 *   <li>COMPRESS_FILE - 压缩文件：zip, 7z, rar, tar, tar.gz, tgz, bz2, gz, xz</li>
 *   <li>TORRENT - 种子文件</li>
 *   <li>FOLDER - 文件夹</li>
 *   <li>DEFAULT - 未知类型</li>
 * </ul>
 */
public class FileUtils {

    /**
     * 根据文件名获取文件类型。
     * 通过文件扩展名判断文件类型，未识别的类型返回DEFAULT。
     *
     * <p>扩展名匹配规则：
     * <ul>
     *   <li>扩展名不区分大小写（会自动转小写比较）</li>
     *   <li>扩展名需要包含点号（如".txt"）</li>
     *   <li>多扩展名只取最后一个（如.tar.gz返回.tar.gz的映射）</li>
     * </ul>
     *
     * @param fileName 文件名
     * @return FileTypeEnum 文件类型枚举
     */
    public static FileTypeEnum getFileType(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return FileTypeEnum.DEFAULT;
        }
        String extensionName = extractFileExtensionName(fileName).toLowerCase();
        return fileTypeMap.getOrDefault(extensionName, FileTypeEnum.DEFAULT);
    }

    /**
     * 获取文件类型的显示名称。
     *
     * @param fileType 文件类型
     * @return 显示名称
     */
    public static String getTypeDisplayName(FileTypeEnum fileType) {
        if (fileType == null) {
            return "未知";
        }
        return typeDisplayNames.getOrDefault(fileType, fileType.name().toLowerCase());
    }

    /**
     * 获取文件类型的显示名称（根据类型字符串）。
     *
     * @param typeName 类型名称
     * @return 显示名称
     */
    public static String getTypeDisplayName(String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return "未知";
        }
        try {
            FileTypeEnum fileType = FileTypeEnum.valueOf(typeName.toUpperCase());
            return getTypeDisplayName(fileType);
        } catch (IllegalArgumentException e) {
            return "未知";
        }
    }

    /**
     * 检查文件类型是否属于指定分类。
     *
     * @param fileType 文件类型
     * @param category 分类名称：video, audio, picture, document, compress, other
     * @return true表示属于该分类
     */
    public static boolean isInCategory(FileTypeEnum fileType, String category) {
        if (fileType == null || category == null) {
            return false;
        }
        Set<FileTypeEnum> categoryTypes = categoryFileTypes.get(category.toLowerCase());
        return categoryTypes != null && categoryTypes.contains(fileType);
    }

    /**
     * 检查文件类型是否属于指定分类（根据类型字符串）。
     *
     * @param typeName 类型名称
     * @param category 分类名称
     * @return true表示属于该分类
     */
    public static boolean isInCategory(String typeName, String category) {
        if (typeName == null) {
            return false;
        }
        try {
            FileTypeEnum fileType = FileTypeEnum.valueOf(typeName.toUpperCase());
            return isInCategory(fileType, category);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 检查文件类型是否有效。
     * 验证文件类型字符串是否为有效的FileType枚举值。
     *
     * @param fileType 文件类型字符串
     * @throws DataFormatException 当文件类型无效时抛出
     */
    public static void checkFileType(String fileType) {
        if (fileType == null || fileType.trim().isEmpty()) {
            throw new DataFormatException(
                    String.format("文件类型不能为空 [有效类型=%s]",
                            Arrays.toString(FileTypeEnum.values())));
        }

        try {
            FileTypeEnum.valueOf(fileType.toUpperCase());
        } catch (Exception e) {
            throw new DataFormatException(
                    String.format("文件类型不正确 [fileType=%s, 有效类型=%s]",
                            fileType, Arrays.toString(FileTypeEnum.values())));
        }
    }

    /**
     * 比较文件类型字符串与FileType枚举是否相等。
     *
     * @param type     文件类型字符串
     * @param fileType FileType枚举
     * @return true表示相等，false表示不相等
     */
    public static boolean equals(String type, FileTypeEnum fileType) {
        return fileType.toString().equals(type);
    }

    /**
     * 验证文件名是否有效。
     * 检查文件名长度是否超过100个字符，以及是否包含非法字符。
     *
     * @param fileName 文件名
     * @return true表示有效，false表示无效
     */
    public static boolean validFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return false;
        }

        // 检查文件名长度
        if (fileName.length() > 100) {
            return false;
        }

        // 检查是否包含Windows不允许的字符
        String invalidChars = "\\/:*?\"<>|";
        for (char c : invalidChars.toCharArray()) {
            if (fileName.indexOf(c) >= 0) {
                return false;
            }
        }

        // 检查是否包含控制字符
        for (char c : fileName.toCharArray()) {
            if (c < 32) {
                return false;
            }
        }

        return true;
    }

    /**
     * 检查文件名是否合法。
     * 验证文件名长度和字符，超过100个字符或包含非法字符则抛出异常。
     *
     * @param fileName 文件名
     * @throws DataFormatException 当文件名不合法时抛出
     */
    public static void checkFileName(String fileName) {
        if (!validFileName(fileName)) {
            if (fileName == null || fileName.trim().isEmpty()) {
                throw new DataFormatException("文件名不能为空");
            } else if (fileName.length() > 100) {
                throw new DataFormatException(
                        String.format("文件名长度超过最大限制 [fileName=%s, 实际长度=%d, 最大长度=100]",
                                fileName, fileName.length()));
            } else {
                throw new DataFormatException(
                        String.format("文件名包含非法字符 [fileName=%s]", fileName));
            }
        }
    }

    /**
     * 提取文件扩展名。
     * 从文件名中提取扩展名，包含点号（如".txt"）。
     * 对于多扩展名的文件（如.tar.gz），只返回最后一个扩展名。
     *
     * @param fileName 文件名
     * @return 文件扩展名，如果没有扩展名则返回空字符串
     */
    public static String extractFileExtensionName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        // 查找最后一个点号的位置
        int lastDotIndex = fileName.lastIndexOf('.');

        // 如果没有找到点号，或者点号是第一个字符（隐藏文件），或者点号是最后一个字符
        if (lastDotIndex <= 0 || lastDotIndex == fileName.length() - 1) {
            return "";
        }

        // 返回扩展名（包含点号）
        return fileName.substring(lastDotIndex);
    }

    /**
     * 安全地解析文件路径，防止路径遍历攻击。
     * 将相对路径基于基础路径解析后，验证结果路径是否仍在基础路径内。
     *
     * @param basePath     基础路径（允许的根目录）
     * @param relativePath 相对路径
     * @return 解析后的安全绝对路径
     * @throws InvalidFilePathException 当解析后的路径超出基础路径范围时抛出
     */
    public static Path resolveSecurePath(String basePath, String relativePath) {
        Path base = Paths.get(basePath).toAbsolutePath().normalize();
        Path resolved = base.resolve(relativePath).normalize();
        if (!resolved.startsWith(base)) {
            throw new InvalidFilePathException("非法文件路径: " + relativePath);
        }
        return resolved;
    }

    /**
     * 文件类型映射表。
     * 键为扩展名（包含点号，小写），值为FileType枚举。
     */
    private static final Map<String, FileTypeEnum> fileTypeMap = new HashMap<>();

    /**
     * 文件类型显示名称映射。
     */
    private static final Map<FileTypeEnum, String> typeDisplayNames = new HashMap<>();

    /**
     * 分类与文件类型映射。
     */
    private static final Map<String, Set<FileTypeEnum>> categoryFileTypes = new HashMap<>();

    static {
        // ========== 初始化文件类型映射 ==========

        // PDF文档
        fileTypeMap.put(".pdf", FileTypeEnum.PDF);

        // 压缩文件
        fileTypeMap.put(".zip", FileTypeEnum.COMPRESS_FILE);
        fileTypeMap.put(".7z", FileTypeEnum.COMPRESS_FILE);
        fileTypeMap.put(".rar", FileTypeEnum.COMPRESS_FILE);
        fileTypeMap.put(".tar", FileTypeEnum.COMPRESS_FILE);
        fileTypeMap.put(".tar.gz", FileTypeEnum.COMPRESS_FILE);
        fileTypeMap.put(".tgz", FileTypeEnum.COMPRESS_FILE);
        fileTypeMap.put(".tar.bz2", FileTypeEnum.COMPRESS_FILE);
        fileTypeMap.put(".bz2", FileTypeEnum.COMPRESS_FILE);
        fileTypeMap.put(".gz", FileTypeEnum.COMPRESS_FILE);
        fileTypeMap.put(".xz", FileTypeEnum.COMPRESS_FILE);

        // 视频文件
        fileTypeMap.put(".mp4", FileTypeEnum.VIDEO);
        fileTypeMap.put(".flv", FileTypeEnum.VIDEO);
        fileTypeMap.put(".rmvb", FileTypeEnum.VIDEO);
        fileTypeMap.put(".avi", FileTypeEnum.VIDEO);
        fileTypeMap.put(".mkv", FileTypeEnum.VIDEO);
        fileTypeMap.put(".mov", FileTypeEnum.VIDEO);
        fileTypeMap.put(".wmv", FileTypeEnum.VIDEO);
        fileTypeMap.put(".webm", FileTypeEnum.VIDEO);
        fileTypeMap.put(".m4v", FileTypeEnum.VIDEO);
        fileTypeMap.put(".3gp", FileTypeEnum.VIDEO);
        fileTypeMap.put(".mpg", FileTypeEnum.VIDEO);
        fileTypeMap.put(".mpeg", FileTypeEnum.VIDEO);
        fileTypeMap.put(".vob", FileTypeEnum.VIDEO);

        // 音频文件
        fileTypeMap.put(".mp3", FileTypeEnum.AUDIO);
        fileTypeMap.put(".wav", FileTypeEnum.AUDIO);
        fileTypeMap.put(".flac", FileTypeEnum.AUDIO);
        fileTypeMap.put(".aac", FileTypeEnum.AUDIO);
        fileTypeMap.put(".ogg", FileTypeEnum.AUDIO);
        fileTypeMap.put(".wma", FileTypeEnum.AUDIO);
        fileTypeMap.put(".m4a", FileTypeEnum.AUDIO);
        fileTypeMap.put(".ape", FileTypeEnum.AUDIO);

        // 图片文件
        fileTypeMap.put(".png", FileTypeEnum.PICTURE);
        fileTypeMap.put(".jpg", FileTypeEnum.PICTURE);
        fileTypeMap.put(".jpeg", FileTypeEnum.PICTURE);
        fileTypeMap.put(".gif", FileTypeEnum.PICTURE);
        fileTypeMap.put(".ico", FileTypeEnum.PICTURE);
        fileTypeMap.put(".bmp", FileTypeEnum.PICTURE);
        fileTypeMap.put(".webp", FileTypeEnum.PICTURE);
        fileTypeMap.put(".svg", FileTypeEnum.PICTURE);
        fileTypeMap.put(".tiff", FileTypeEnum.PICTURE);
        fileTypeMap.put(".tif", FileTypeEnum.PICTURE);
        fileTypeMap.put(".psd", FileTypeEnum.PICTURE);
        fileTypeMap.put(".raw", FileTypeEnum.PICTURE);

        // Word文档
        fileTypeMap.put(".doc", FileTypeEnum.DOC);
        fileTypeMap.put(".docx", FileTypeEnum.DOC);
        fileTypeMap.put(".xls", FileTypeEnum.DOC);
        fileTypeMap.put(".xlsx", FileTypeEnum.DOC);
        fileTypeMap.put(".pages", FileTypeEnum.DOC);
        fileTypeMap.put(".numbers", FileTypeEnum.DOC);
        fileTypeMap.put(".key", FileTypeEnum.DOC);
        fileTypeMap.put(".odt", FileTypeEnum.DOC);
        fileTypeMap.put(".ods", FileTypeEnum.DOC);

        // 纯文本文件
        fileTypeMap.put(".txt", FileTypeEnum.TXT);
        fileTypeMap.put(".md", FileTypeEnum.TXT);
        fileTypeMap.put(".markdown", FileTypeEnum.TXT);
        fileTypeMap.put(".json", FileTypeEnum.TXT);
        fileTypeMap.put(".xml", FileTypeEnum.TXT);
        fileTypeMap.put(".yaml", FileTypeEnum.TXT);
        fileTypeMap.put(".yml", FileTypeEnum.TXT);
        fileTypeMap.put(".log", FileTypeEnum.TXT);
        fileTypeMap.put(".ini", FileTypeEnum.TXT);
        fileTypeMap.put(".cfg", FileTypeEnum.TXT);
        fileTypeMap.put(".conf", FileTypeEnum.TXT);
        fileTypeMap.put(".properties", FileTypeEnum.TXT);

        // PPT演示文稿
        fileTypeMap.put(".ppt", FileTypeEnum.PPT);
        fileTypeMap.put(".pptx", FileTypeEnum.PPT);
        fileTypeMap.put(".keynote", FileTypeEnum.PPT);

        // 种子文件
        fileTypeMap.put(".torrent", FileTypeEnum.TORRENT);

        // 网页文件
        fileTypeMap.put(".html", FileTypeEnum.WEB);
        fileTypeMap.put(".htm", FileTypeEnum.WEB);
        fileTypeMap.put(".css", FileTypeEnum.WEB);
        fileTypeMap.put(".scss", FileTypeEnum.WEB);
        fileTypeMap.put(".sass", FileTypeEnum.WEB);
        fileTypeMap.put(".less", FileTypeEnum.WEB);

        // 代码文件
        fileTypeMap.put(".js", FileTypeEnum.CODE);
        fileTypeMap.put(".ts", FileTypeEnum.CODE);
        fileTypeMap.put(".tsx", FileTypeEnum.CODE);
        fileTypeMap.put(".jsx", FileTypeEnum.CODE);
        fileTypeMap.put(".java", FileTypeEnum.CODE);
        fileTypeMap.put(".c", FileTypeEnum.CODE);
        fileTypeMap.put(".cpp", FileTypeEnum.CODE);
        fileTypeMap.put(".h", FileTypeEnum.CODE);
        fileTypeMap.put(".hpp", FileTypeEnum.CODE);
        fileTypeMap.put(".cs", FileTypeEnum.CODE);
        fileTypeMap.put(".py", FileTypeEnum.CODE);
        fileTypeMap.put(".go", FileTypeEnum.CODE);
        fileTypeMap.put(".rs", FileTypeEnum.CODE);
        fileTypeMap.put(".rb", FileTypeEnum.CODE);
        fileTypeMap.put(".php", FileTypeEnum.CODE);
        fileTypeMap.put(".swift", FileTypeEnum.CODE);
        fileTypeMap.put(".kt", FileTypeEnum.CODE);
        fileTypeMap.put(".kts", FileTypeEnum.CODE);
        fileTypeMap.put(".scala", FileTypeEnum.CODE);
        fileTypeMap.put(".vue", FileTypeEnum.CODE);
        fileTypeMap.put(".jsx", FileTypeEnum.CODE);
        fileTypeMap.put(".tsx", FileTypeEnum.CODE);
        fileTypeMap.put(".sh", FileTypeEnum.CODE);
        fileTypeMap.put(".bash", FileTypeEnum.CODE);
        fileTypeMap.put(".zsh", FileTypeEnum.CODE);
        fileTypeMap.put(".bat", FileTypeEnum.CODE);
        fileTypeMap.put(".ps1", FileTypeEnum.CODE);
        fileTypeMap.put(".sql", FileTypeEnum.CODE);
        fileTypeMap.put(".r", FileTypeEnum.CODE);
        fileTypeMap.put(".lua", FileTypeEnum.CODE);
        fileTypeMap.put(".pl", FileTypeEnum.CODE);
        fileTypeMap.put(".pm", FileTypeEnum.CODE);

        // ========== 初始化显示名称 ==========
        typeDisplayNames.put(FileTypeEnum.DEFAULT, "未知");
        typeDisplayNames.put(FileTypeEnum.FOLDER, "文件夹");
        typeDisplayNames.put(FileTypeEnum.VIDEO, "视频");
        typeDisplayNames.put(FileTypeEnum.AUDIO, "音频");
        typeDisplayNames.put(FileTypeEnum.PICTURE, "图片");
        typeDisplayNames.put(FileTypeEnum.DOC, "文档");
        typeDisplayNames.put(FileTypeEnum.PDF, "PDF");
        typeDisplayNames.put(FileTypeEnum.TXT, "文本");
        typeDisplayNames.put(FileTypeEnum.PPT, "演示");
        typeDisplayNames.put(FileTypeEnum.CODE, "代码");
        typeDisplayNames.put(FileTypeEnum.WEB, "网页");
        typeDisplayNames.put(FileTypeEnum.COMPRESS_FILE, "压缩");
        typeDisplayNames.put(FileTypeEnum.TORRENT, "种子");

        // ========== 初始化分类映射 ==========
        Set<FileTypeEnum> videoTypes = new HashSet<>();
        videoTypes.add(FileTypeEnum.VIDEO);
        categoryFileTypes.put("video", videoTypes);

        Set<FileTypeEnum> audioTypes = new HashSet<>();
        audioTypes.add(FileTypeEnum.AUDIO);
        categoryFileTypes.put("audio", audioTypes);

        Set<FileTypeEnum> pictureTypes = new HashSet<>();
        pictureTypes.add(FileTypeEnum.PICTURE);
        categoryFileTypes.put("picture", pictureTypes);

        Set<FileTypeEnum> documentTypes = new HashSet<>();
        documentTypes.add(FileTypeEnum.DOC);
        documentTypes.add(FileTypeEnum.PDF);
        documentTypes.add(FileTypeEnum.TXT);
        documentTypes.add(FileTypeEnum.PPT);
        documentTypes.add(FileTypeEnum.CODE);
        documentTypes.add(FileTypeEnum.WEB);
        categoryFileTypes.put("document", documentTypes);

        Set<FileTypeEnum> compressTypes = new HashSet<>();
        compressTypes.add(FileTypeEnum.COMPRESS_FILE);
        categoryFileTypes.put("compress", compressTypes);

        Set<FileTypeEnum> otherTypes = new HashSet<>();
        otherTypes.add(FileTypeEnum.TORRENT);
        otherTypes.add(FileTypeEnum.DEFAULT);
        categoryFileTypes.put("other", otherTypes);
    }

    private FileUtils() {
    }
}
