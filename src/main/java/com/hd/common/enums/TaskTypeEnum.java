package com.hd.common.enums;

import lombok.Getter;

/**
 * 任务类型枚举
 *
 * @author xhx
 * @since 2026-06-03
 */
@Getter
public enum TaskTypeEnum {
    SCAN_DUPLICATE("SCAN_DUPLICATE", "重复文件扫描"),
    STORAGE_ANALYSIS("STORAGE_ANALYSIS", "存储分析"),
    INTEGRITY_CHECK("INTEGRITY_CHECK", "完整性检查"),
    METADATA_EXTRACT("METADATA_EXTRACT", "元数据提取"),
    THUMBNAIL_GENERATE("THUMBNAIL_GENERATE", "缩略图生成");

    private final String code;
    private final String name;

    TaskTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getNameByCode(String code) {
        if (code == null) {
            return "";
        }
        for (TaskTypeEnum type : values()) {
            if (type.code.equalsIgnoreCase(code.trim())) {
                return type.name;
            }
        }
        return code;
    }
}
