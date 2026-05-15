package com.baoying.vehicleapplication.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TemplateListItem {
    private Integer templateId;
    private String name;
    private String description;
    private Integer type;
    private String typeName;
    private Integer status;
    private String createByName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer nodeCount;      // 节点数量
}