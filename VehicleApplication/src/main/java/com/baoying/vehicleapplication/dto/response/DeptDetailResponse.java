package com.baoying.vehicleapplication.dto.response;

import lombok.Data;

@Data
public class DeptDetailResponse {
    private Integer id;
    private String name;
    private Integer parentId;
    private String parentName;    // 上级部门名称
    private Integer sort;
    private String description;
    private Integer status;
    private Long createBy;
    private String createByName;
    private String createTime;
    private Integer userCount;     // 部门下账号数量
}