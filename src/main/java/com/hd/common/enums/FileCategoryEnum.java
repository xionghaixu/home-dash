package com.hd.common.enums;

import lombok.Getter;

/**
 * 文件大类分类枚举
 *
 * @author xhx
 * @since 2026-06-03
 */
@Getter
public enum FileCategoryEnum {
    PICTURE("picture", "图片"),
    VIDEO("video", "视频"),
    AUDIO("audio", "音频"),
    DOCUMENT("document", "文档"),
    COMPRESS("compress", "压缩包"),
    OTHER("other", "其他");

    private final String code;
    private final String name;

    FileCategoryEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static FileCategoryEnum fromCode(String code) {
        if (code == null) {
            return OTHER;
        }
        for (FileCategoryEnum category : values()) {
            if (category.code.equalsIgnoreCase(code.trim())) {
                return category;
            }
        }
        return OTHER;
    }
}
