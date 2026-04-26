package com.hd.model.vo;

import lombok.*;

/**
 * 预览状态VO。
 * 描述文件的预览能力及状态。
 *
 * @author team-lead
 * @version 1.0
 * @createTime 2026/04/25
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreviewStatusVo {

    /** 是否可预览。 */
    private Boolean canPreview;

    /** 预览类型：IMAGE/TEXT/AUDIO/VIDEO/NONE。 */
    private String previewType;

    /** 预览状态：READY/LOADING/ERROR/UNSUPPORTED。 */
    private String previewStatus;

    /** 错误信息。 */
    private String errorMessage;

    /** 预览类型常量。 */
    public static final String TYPE_IMAGE = "IMAGE";
    public static final String TYPE_TEXT = "TEXT";
    public static final String TYPE_AUDIO = "AUDIO";
    public static final String TYPE_VIDEO = "VIDEO";
    public static final String TYPE_NONE = "NONE";

    /** 预览状态常量。 */
    public static final String STATUS_READY = "READY";
    public static final String STATUS_LOADING = "LOADING";
    public static final String STATUS_ERROR = "ERROR";
    public static final String STATUS_UNSUPPORTED = "UNSUPPORTED";
}