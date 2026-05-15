package com.baoying.vehicleapplication.common;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public enum VehicleTypeEnum {

    CAR(1, "轿车"),
    BUSINESS(2, "商务车"),
    BUS(3, "大巴"),
    MINIBUS(4, "小巴"),
    OTHER(5, "其他");

    private final Integer code;
    private final String name;

    VehicleTypeEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getNameByCode(Integer code) {
        if (code == null) return null;
        for (VehicleTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type.name;
            }
        }
        return "未知";
    }
    public static List<Integer> getAvailableTypesByTemplateType(Integer templateType) {
        List<Integer> result = new ArrayList<>();
        if (templateType == null) {
            return result;
        }
        switch (templateType) {
            case 1:  // 内部用车
                result.add(CAR.getCode());
                result.add(BUSINESS.getCode());
                result.add(OTHER.getCode());
                break;
            case 2:  // 跨部门用车
                result.add(CAR.getCode());
                result.add(BUSINESS.getCode());
                result.add(OTHER.getCode());
                break;
            case 3:  // 长途用车
                result.add(BUS.getCode());
                result.add(MINIBUS.getCode());
                result.add(OTHER.getCode());
                break;
            default:
                break;
        }
        return result;
    }
    public static boolean isValidForTemplateType(Integer vehicleType, Integer templateType) {
        List<Integer> availableTypes = getAvailableTypesByTemplateType(templateType);
        return availableTypes.contains(vehicleType);
    }
}