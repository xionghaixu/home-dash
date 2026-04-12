package site.bitinit.pnd.web.util;

import site.bitinit.pnd.web.config.FileType;
import site.bitinit.pnd.web.exception.DataFormatException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 文件工具类。
 * 提供文件类型判断、文件名验证、扩展名提取等文件相关操作方法。
 *
 * @author john
 * @date 2020-01-11
 */
public class FileUtils {

    /**
     * 根据文件名获取文件类型
     * 
     * @param fileName 文件名
     * @return FileType
     */
    /**
     * 根据文件名获取文件类型。
     * 通过文件扩展名判断文件类型，未识别的类型返回DEFAULT。
     *
     * @param fileName 文件名
     * @return FileType 文件类型枚举
     */
    public static FileType getFileType(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return FileType.DEFAULT;
        }
        String extensionName = extractFileExtensionName(fileName);
        return fileTypeMap.getOrDefault(extensionName, FileType.DEFAULT);
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
                            java.util.Arrays.toString(FileType.values())));
        }
        
        try {
            FileType.valueOf(fileType.toUpperCase());
        } catch (Exception e) {
            throw new DataFormatException(
                    String.format("文件类型不正确 [fileType=%s, 有效类型=%s]",
                            fileType, java.util.Arrays.toString(FileType.values())));
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

    private static Map<String, FileType> fileTypeMap = new HashMap<>();

    static {
        // pdf
        fileTypeMap.put(".pdf", FileType.PDF);

        // compress_file
        fileTypeMap.put(".tar.gz", FileType.COMPRESS_FILE);
        fileTypeMap.put(".zip", FileType.COMPRESS_FILE);
        fileTypeMap.put(".7z", FileType.COMPRESS_FILE);
        fileTypeMap.put(".rar", FileType.COMPRESS_FILE);

        // video
        fileTypeMap.put(".mp4", FileType.VIDEO);
        fileTypeMap.put(".flv", FileType.VIDEO);
        fileTypeMap.put(".rmvb", FileType.VIDEO);
        fileTypeMap.put(".avi", FileType.VIDEO);
        fileTypeMap.put(".mkv", FileType.VIDEO);

        // audio
        fileTypeMap.put(".mp3", FileType.AUDIO);

        // picture
        fileTypeMap.put(".png", FileType.PICTURE);
        fileTypeMap.put(".jpg", FileType.PICTURE);
        fileTypeMap.put(".jpeg", FileType.PICTURE);
        fileTypeMap.put(".gif", FileType.PICTURE);
        fileTypeMap.put(".ico", FileType.PICTURE);

        // doc
        fileTypeMap.put(".doc", FileType.DOC);
        fileTypeMap.put(".docx", FileType.DOC);

        // txt
        fileTypeMap.put(".txt", FileType.TXT);

        // ppt
        fileTypeMap.put(".ppt", FileType.PPT);
        fileTypeMap.put(".pptx", FileType.PPT);

        // torrent
        fileTypeMap.put(".torrent", FileType.TORRENT);

        // web
        fileTypeMap.put(".html", FileType.WEB);
        fileTypeMap.put(".htm", FileType.WEB);

        // code
        fileTypeMap.put(".js", FileType.CODE);
        fileTypeMap.put(".json", FileType.CODE);
        fileTypeMap.put(".java", FileType.CODE);
        fileTypeMap.put(".c", FileType.CODE);
        fileTypeMap.put(".cpp", FileType.CODE);
        fileTypeMap.put(".h", FileType.CODE);
        fileTypeMap.put(".py", FileType.CODE);
        fileTypeMap.put(".go", FileType.CODE);
        fileTypeMap.put(".vue", FileType.CODE);
    }

    private FileUtils() {
    }
}
