package com.baoying.vehicleapplication.dto.request;

import lombok.Data;

@Data
public class DeptRequest {
    private Integer id;          // 编辑时传入
    private String name;
    private Integer parentId;
    private Integer sort;
    private String description;
    private Integer status;
}