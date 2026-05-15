package com.baoying.vehicleapplication.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class DeptTreeResponse {
    private Integer id;
    private String name;
    private Integer parentId;
    private Integer sort;
    private String description;
    private Integer status;
    private Long createBy;
    private String createByName;
    private String createTime;
    private List<DeptTreeResponse> children;
}