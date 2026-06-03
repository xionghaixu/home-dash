package com.hd.common.enums;

import lombok.Getter;

/**
 * 任务状态枚举
 *
 * @author xhx
 * @since 2026-06-03
 */
@Getter
public enum TaskStatusEnum {
    PENDING("PENDING", "等待中"),
    RUNNING("RUNNING", "执行中"),
    SUCCESS("SUCCESS", "已成功"),
    FAILED("FAILED", "已失败");

    private final String code;
    private final String name;

    TaskStatusEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static TaskStatusEnum fromCode(String code) {
        if (code == null) {
            return PENDING;
        }
        for (TaskStatusEnum status : values()) {
            if (status.code.equalsIgnoreCase(code.trim())) {
                return status;
            }
        }
        return PENDING;
    }
}
