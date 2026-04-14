package site.bitinit.pnd.web.util;

import site.bitinit.pnd.web.config.FileType;
import site.bitinit.pnd.web.exception.DataFormatException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 文件工具类。
 * 提供文件类型判断、文件名验证、扩展名提取等文件相关操作方法。
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
 *
 * @author john
 * @date 2020-01-11
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
     * @return FileType 文件类型枚举
     */
    public static FileType getFileType(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return FileType.DEFAULT;
        }
        String extensionName = extractFileExtensionName(fileName).toLowerCase();
        return fileTypeMap.getOrDefault(extensionName, FileType.DEFAULT);
    }

    /**
     * 获取文件类型的显示名称。
     *
     * @param fileType 文件类型
     * @return 显示名称
     */
    public static String getTypeDisplayName(FileType fileType) {
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
            FileType fileType = FileType.valueOf(typeName.toUpperCase());
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
    public static boolean isInCategory(FileType fileType, String category) {
        if (fileType == null || category == null) {
            return false;
        }
        Set<FileType> categoryTypes = categoryFileTypes.get(category.toLowerCase());
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
            FileType fileType = FileType.valueOf(typeName.toUpperCase());
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
                            Arrays.toString(FileType.values())));
        }

        try {
            FileType.valueOf(fileType.toUpperCase());
        } catch (Exception e) {
            throw new DataFormatException(
                    String.format("文件类型不正确 [fileType=%s, 有效类型=%s]",
                            fileType, Arrays.toString(FileType.values())));
        }
    }

    /**
     * 比较文件类型字符串与FileType枚举是否相等。
     *
     * @param type     文件类型字符串
     * @param fileType FileType枚举
     * @return true表示相等，false表示不相等
     */
    public static boolean equals(String type, FileType fileType) {
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
     * 文件类型映射表。
     * 键为扩展名（包含点号，小写），值为FileType枚举。
     */
    private static final Map<String, FileType> fileTypeMap = new HashMap<>();

    /**
     * 文件类型显示名称映射。
     */
    private static final Map<FileType, String> typeDisplayNames = new HashMap<>();

    /**
     * 分类与文件类型映射。
     */
    private static final Map<String, Set<FileType>> categoryFileTypes = new HashMap<>();

    static {
        // ========== 初始化文件类型映射 ==========

        // PDF文档
        fileTypeMap.put(".pdf", FileType.PDF);

        // 压缩文件
        fileTypeMap.put(".zip", FileType.COMPRESS_FILE);
        fileTypeMap.put(".7z", FileType.COMPRESS_FILE);
        fileTypeMap.put(".rar", FileType.COMPRESS_FILE);
        fileTypeMap.put(".tar", FileType.COMPRESS_FILE);
        fileTypeMap.put(".tar.gz", FileType.COMPRESS_FILE);
        fileTypeMap.put(".tgz", FileType.COMPRESS_FILE);
        fileTypeMap.put(".tar.bz2", FileType.COMPRESS_FILE);
        fileTypeMap.put(".bz2", FileType.COMPRESS_FILE);
        fileTypeMap.put(".gz", FileType.COMPRESS_FILE);
        fileTypeMap.put(".xz", FileType.COMPRESS_FILE);

        // 视频文件
        fileTypeMap.put(".mp4", FileType.VIDEO);
        fileTypeMap.put(".flv", FileType.VIDEO);
        fileTypeMap.put(".rmvb", FileType.VIDEO);
        fileTypeMap.put(".avi", FileType.VIDEO);
        fileTypeMap.put(".mkv", FileType.VIDEO);
        fileTypeMap.put(".mov", FileType.VIDEO);
        fileTypeMap.put(".wmv", FileType.VIDEO);
        fileTypeMap.put(".webm", FileType.VIDEO);
        fileTypeMap.put(".m4v", FileType.VIDEO);
        fileTypeMap.put(".3gp", FileType.VIDEO);
        fileTypeMap.put(".mpg", FileType.VIDEO);
        fileTypeMap.put(".mpeg", FileType.VIDEO);
        fileTypeMap.put(".vob", FileType.VIDEO);

        // 音频文件
        fileTypeMap.put(".mp3", FileType.AUDIO);
        fileTypeMap.put(".wav", FileType.AUDIO);
        fileTypeMap.put(".flac", FileType.AUDIO);
        fileTypeMap.put(".aac", FileType.AUDIO);
        fileTypeMap.put(".ogg", FileType.AUDIO);
        fileTypeMap.put(".wma", FileType.AUDIO);
        fileTypeMap.put(".m4a", FileType.AUDIO);
        fileTypeMap.put(".ape", FileType.AUDIO);

        // 图片文件
        fileTypeMap.put(".png", FileType.PICTURE);
        fileTypeMap.put(".jpg", FileType.PICTURE);
        fileTypeMap.put(".jpeg", FileType.PICTURE);
        fileTypeMap.put(".gif", FileType.PICTURE);
        fileTypeMap.put(".ico", FileType.PICTURE);
        fileTypeMap.put(".bmp", FileType.PICTURE);
        fileTypeMap.put(".webp", FileType.PICTURE);
        fileTypeMap.put(".svg", FileType.PICTURE);
        fileTypeMap.put(".tiff", FileType.PICTURE);
        fileTypeMap.put(".tif", FileType.PICTURE);
        fileTypeMap.put(".psd", FileType.PICTURE);
        fileTypeMap.put(".raw", FileType.PICTURE);

        // Word文档
        fileTypeMap.put(".doc", FileType.DOC);
        fileTypeMap.put(".docx", FileType.DOC);
        fileTypeMap.put(".xls", FileType.DOC);
        fileTypeMap.put(".xlsx", FileType.DOC);
        fileTypeMap.put(".pages", FileType.DOC);
        fileTypeMap.put(".numbers", FileType.DOC);
        fileTypeMap.put(".key", FileType.DOC);
        fileTypeMap.put(".odt", FileType.DOC);
        fileTypeMap.put(".ods", FileType.DOC);

        // 纯文本文件
        fileTypeMap.put(".txt", FileType.TXT);
        fileTypeMap.put(".md", FileType.TXT);
        fileTypeMap.put(".markdown", FileType.TXT);
        fileTypeMap.put(".json", FileType.TXT);
        fileTypeMap.put(".xml", FileType.TXT);
        fileTypeMap.put(".yaml", FileType.TXT);
        fileTypeMap.put(".yml", FileType.TXT);
        fileTypeMap.put(".log", FileType.TXT);
        fileTypeMap.put(".ini", FileType.TXT);
        fileTypeMap.put(".cfg", FileType.TXT);
        fileTypeMap.put(".conf", FileType.TXT);
        fileTypeMap.put(".properties", FileType.TXT);

        // PPT演示文稿
        fileTypeMap.put(".ppt", FileType.PPT);
        fileTypeMap.put(".pptx", FileType.PPT);
        fileTypeMap.put(".keynote", FileType.PPT);

        // 种子文件
        fileTypeMap.put(".torrent", FileType.TORRENT);

        // 网页文件
        fileTypeMap.put(".html", FileType.WEB);
        fileTypeMap.put(".htm", FileType.WEB);
        fileTypeMap.put(".css", FileType.WEB);
        fileTypeMap.put(".scss", FileType.WEB);
        fileTypeMap.put(".sass", FileType.WEB);
        fileTypeMap.put(".less", FileType.WEB);

        // 代码文件
        fileTypeMap.put(".js", FileType.CODE);
        fileTypeMap.put(".ts", FileType.CODE);
        fileTypeMap.put(".tsx", FileType.CODE);
        fileTypeMap.put(".jsx", FileType.CODE);
        fileTypeMap.put(".json", FileType.CODE);
        fileTypeMap.put(".java", FileType.CODE);
        fileTypeMap.put(".c", FileType.CODE);
        fileTypeMap.put(".cpp", FileType.CODE);
        fileTypeMap.put(".h", FileType.CODE);
        fileTypeMap.put(".hpp", FileType.CODE);
        fileTypeMap.put(".cs", FileType.CODE);
        fileTypeMap.put(".py", FileType.CODE);
        fileTypeMap.put(".go", FileType.CODE);
        fileTypeMap.put(".rs", FileType.CODE);
        fileTypeMap.put(".rb", FileType.CODE);
        fileTypeMap.put(".php", FileType.CODE);
        fileTypeMap.put(".swift", FileType.CODE);
        fileTypeMap.put(".kt", FileType.CODE);
        fileTypeMap.put(".kts", FileType.CODE);
        fileTypeMap.put(".scala", FileType.CODE);
        fileTypeMap.put(".vue", FileType.CODE);
        fileTypeMap.put(".jsx", FileType.CODE);
        fileTypeMap.put(".tsx", FileType.CODE);
        fileTypeMap.put(".sh", FileType.CODE);
        fileTypeMap.put(".bash", FileType.CODE);
        fileTypeMap.put(".zsh", FileType.CODE);
        fileTypeMap.put(".bat", FileType.CODE);
        fileTypeMap.put(".ps1", FileType.CODE);
        fileTypeMap.put(".sql", FileType.CODE);
        fileTypeMap.put(".r", FileType.CODE);
        fileTypeMap.put(".lua", FileType.CODE);
        fileTypeMap.put(".pl", FileType.CODE);
        fileTypeMap.put(".pm", FileType.CODE);

        // ========== 初始化显示名称 ==========
        typeDisplayNames.put(FileType.DEFAULT, "未知");
        typeDisplayNames.put(FileType.FOLDER, "文件夹");
        typeDisplayNames.put(FileType.VIDEO, "视频");
        typeDisplayNames.put(FileType.AUDIO, "音频");
        typeDisplayNames.put(FileType.PICTURE, "图片");
        typeDisplayNames.put(FileType.DOC, "文档");
        typeDisplayNames.put(FileType.PDF, "PDF");
        typeDisplayNames.put(FileType.TXT, "文本");
        typeDisplayNames.put(FileType.PPT, "演示");
        typeDisplayNames.put(FileType.CODE, "代码");
        typeDisplayNames.put(FileType.WEB, "网页");
        typeDisplayNames.put(FileType.COMPRESS_FILE, "压缩");
        typeDisplayNames.put(FileType.TORRENT, "种子");

        // ========== 初始化分类映射 ==========
        Set<FileType> videoTypes = new HashSet<>();
        videoTypes.add(FileType.VIDEO);
        categoryFileTypes.put("video", videoTypes);

        Set<FileType> audioTypes = new HashSet<>();
        audioTypes.add(FileType.AUDIO);
        categoryFileTypes.put("audio", audioTypes);

        Set<FileType> pictureTypes = new HashSet<>();
        pictureTypes.add(FileType.PICTURE);
        categoryFileTypes.put("picture", pictureTypes);

        Set<FileType> documentTypes = new HashSet<>();
        documentTypes.add(FileType.DOC);
        documentTypes.add(FileType.PDF);
        documentTypes.add(FileType.TXT);
        documentTypes.add(FileType.PPT);
        documentTypes.add(FileType.CODE);
        documentTypes.add(FileType.WEB);
        categoryFileTypes.put("document", documentTypes);

        Set<FileType> compressTypes = new HashSet<>();
        compressTypes.add(FileType.COMPRESS_FILE);
        categoryFileTypes.put("compress", compressTypes);

        Set<FileType> otherTypes = new HashSet<>();
        otherTypes.add(FileType.TORRENT);
        otherTypes.add(FileType.DEFAULT);
        categoryFileTypes.put("other", otherTypes);
    }

    private FileUtils() {
    }
}
