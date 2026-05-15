package com.baoying.vehicleapplication.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("process_apply")
public class CarApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private LocalDate startTime;
    private LocalDate endTime;
    private String reason;
    private Integer passengers;
    private String destination;
    private Integer vehicleType;
    private String attachment;
    private Long applyBy;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    private Integer deptId;
    private Integer targetDeptId;  // 新增：跨部门用车时的目标部门ID
    private Integer templateId;
    private Integer currentNode;
    private String currentApproverIds;
    private Integer status;
    private Integer isUrgent;
    private String nodeConfigSnapshot;  // 提交时的模板节点配置快照
}