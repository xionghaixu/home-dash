package com.hd.common.exception;

import com.hd.common.enums.ErrorCode;

/**
 * @author xhx
 * @version 1.0
 * @package com.hd.common.exception
 * @createTime 2026/04/23 23:34
 * @description 文件已存在异常。当尝试创建已存在的文件或文件夹时抛出此异常。继承自BusinessException，表示业务层面的文件冲突错误。
 */
public class FileAlreadyExistsException extends BusinessException {

    private static final long serialVersionUID = -1731288043175680001L;

    private final Long parentId;
    private final String fileName;
    private final Long existingFileId;
    private final String suggestedName;

    /**
     * 默认构造函数。
     */
    public FileAlreadyExistsException() {
        super(ErrorCode.FILE_ALREADY_EXISTS);
        this.parentId = null;
        this.fileName = null;
        this.existingFileId = null;
        this.suggestedName = null;
    }

    /**
     * 构造函数，指定异常消息。
     *
     * @param message 异常消息
     */
    public FileAlreadyExistsException(String message) {
        super(ErrorCode.FILE_ALREADY_EXISTS, message);
        this.parentId = null;
        this.fileName = null;
        this.existingFileId = null;
        this.suggestedName = null;
    }

    /**
     * 构造函数，指定异常消息和原因。
     *
     * @param message 异常消息
     * @param cause   异常原因
     */
    public FileAlreadyExistsException(String message, Throwable cause) {
        super(ErrorCode.FILE_ALREADY_EXISTS, message, cause);
        this.parentId = null;
        this.fileName = null;
        this.existingFileId = null;
        this.suggestedName = null;
    }

    /**
     * 构造函数，指定异常原因。
     *
     * @param cause 异常原因
     */
    public FileAlreadyExistsException(Throwable cause) {
        super(ErrorCode.FILE_ALREADY_EXISTS, cause);
        this.parentId = null;
        this.fileName = null;
        this.existingFileId = null;
        this.suggestedName = null;
    }

    /**
     * 完整构造函数，包含冲突详情。
     *
     * @param parentId       父目录ID
     * @param fileName       冲突的文件名
     * @param existingFileId 已存在文件的ID
     * @param suggestedName  建议的新文件名
     */
    public FileAlreadyExistsException(Long parentId, String fileName,
            Long existingFileId, String suggestedName) {
        super(ErrorCode.FILE_ALREADY_EXISTS,
                buildMessage(parentId, fileName, existingFileId, suggestedName));
        this.parentId = parentId;
        this.fileName = fileName;
        this.existingFileId = existingFileId;
        this.suggestedName = suggestedName;
    }

    /**
     * 完整构造函数，包含冲突详情和自定义消息。
     *
     * @param message        自定义消息
     * @param parentId       父目录ID
     * @param fileName       冲突的文件名
     * @param existingFileId 已存在文件的ID
     * @param suggestedName  建议的新文件名
     */
    public FileAlreadyExistsException(String message, Long parentId,
            String fileName, Long existingFileId, String suggestedName) {
        super(ErrorCode.FILE_ALREADY_EXISTS, message);
        this.parentId = parentId;
        this.fileName = fileName;
        this.existingFileId = existingFileId;
        this.suggestedName = suggestedName;
    }

    /**
     * 获取父目录ID。
     *
     * @return 父目录ID
     */
    public Long getParentId() {
        return parentId;
    }

    /**
     * 获取冲突的文件名。
     *
     * @return 文件名
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * 获取已存在文件的ID。
     *
     * @return 文件ID
     */
    public Long getExistingFileId() {
        return existingFileId;
    }

    /**
     * 获取建议的新文件名。
     *
     * @return 建议的文件名
     */
    public String getSuggestedName() {
        return suggestedName;
    }

    /**
     * 构建异常消息。
     */
    private static String buildMessage(Long parentId, String fileName,
            Long existingFileId, String suggestedName) {
        StringBuilder sb = new StringBuilder();
        sb.append("文件已存在 [parentId=").append(parentId)
                .append(", fileName=").append(fileName);
        if (existingFileId != null) {
            sb.append(", existingFileId=").append(existingFileId);
        }
        if (suggestedName != null) {
            sb.append(", suggestedName=").append(suggestedName);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 创建带有建议文件名的异常。
     *
     * @param parentId      父目录ID
     * @param fileName      冲突的文件名
     * @param suggestedName 建议的新文件名
     * @return 异常实例
     */
    public static FileAlreadyExistsException withSuggestion(
            Long parentId, String fileName, String suggestedName) {
        return new FileAlreadyExistsException(
                String.format("文件已存在，建议使用名称：%s [parentId=%d, fileName=%s]",
                        suggestedName, parentId, fileName),
                parentId, fileName, null, suggestedName);
    }
}
