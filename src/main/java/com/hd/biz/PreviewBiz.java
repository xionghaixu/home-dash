package com.hd.biz;

import java.util.Map;

/**
 * 预览业务接口。
 * 定义文件预览的核心能力，包括图片、文本、音频预览。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
public interface PreviewBiz {

    /**
     * 获取图片缩略图。
     *
     * @param resourceId 资源ID
     * @return 缩略图字节数据，如果不存在返回null
     */
    byte[] getImageThumbnail(Long resourceId);

    /**
     * 获取原始图片。
     *
     * @param resourceId 资源ID
     * @return 原始图片字节数据，如果不存在返回null
     */
    byte[] getOriginalImage(Long resourceId);

    /**
     * 获取图片EXIF信息。
     *
     * @param resourceId 资源ID
     * @return EXIF信息Map
     */
    Map<String, Object> getImageExif(Long resourceId);

    /**
     * 获取图片基本信息（尺寸、拍摄时间等）。
     *
     * @param resourceId 资源ID
     * @return 图片信息Map
     */
    Map<String, Object> getImageInfo(Long resourceId);

    /**
     * 获取文本内容预览。
     *
     * @param resourceId 资源ID
     * @param offset 起始位置
     * @param limit 读取长度
     * @return 文本预览信息Map
     */
    Map<String, Object> getTextPreview(Long resourceId, Long offset, Long limit);

    /**
     * 获取文本摘要。
     *
     * @param resourceId 资源ID
     * @return 文本摘要信息Map
     */
    Map<String, Object> getTextSummary(Long resourceId);

    /**
     * 获取音频元数据。
     *
     * @param resourceId 资源ID
     * @return 音频元数据Map
     */
    Map<String, Object> getAudioMetadata(Long resourceId);

    /**
     * 记录最近使用。
     *
     * @param resourceId 资源ID
     * @param useType 使用类型（PREVIEW/PLAY）
     */
    void recordRecentUse(Long resourceId, String useType);
}
