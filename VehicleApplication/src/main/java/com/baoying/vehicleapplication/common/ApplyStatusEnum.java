package com.baoying.vehicleapplication.common;

import lombok.Getter;

@Getter
public enum ApplyStatusEnum {

    DRAFT(0, "待提交"),
    PENDING(1, "待审批"),
    PROCESSING(2, "审批中"),
    APPROVED(3, "已通过"),
    REJECTED(4, "已驳回"),
    CANCELLED(5, "已撤销"),
    REJECTED_END(6, "已驳回(不可提交)");

    private final Integer code;
    private final String name;

    ApplyStatusEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getNameByCode(Integer code) {
        if (code == null) return null;
        for (ApplyStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status.name;
            }
        }
        return "未知";
    }
}