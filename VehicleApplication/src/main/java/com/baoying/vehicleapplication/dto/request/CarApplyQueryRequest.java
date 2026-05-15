package com.baoying.vehicleapplication.dto.request;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CarApplyQueryRequest {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private Integer status;           // 审批状态
    private LocalDateTime startTime;  // 申请时间范围-开始
    private LocalDateTime endTime;    // 申请时间范围-结束
    private String applicantName;     // 申请人姓名（管理员用）
    private Integer deptId;           // 部门ID（管理员用）
    private Integer templateId;       // 流程模板ID
}