package com.baoying.vehicleapplication.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName(value = "process_template", autoResultMap = true)
public class ProcessTemplate {
    @TableId(type = IdType.AUTO)
    private Integer templateId;
    private String name;
    private String description;
    private Integer type;          // 1内部用车 2跨部门用车 3长途用车
    private String nodeConfig;     // JSON格式存储
    private Integer status;        // 1启用 0禁用
    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}