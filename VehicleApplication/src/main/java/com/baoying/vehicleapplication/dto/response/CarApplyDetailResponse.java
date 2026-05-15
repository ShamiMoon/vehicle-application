package com.baoying.vehicleapplication.dto.response;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CarApplyDetailResponse {
    private Long id;
    private String title;
    private LocalDate startTime;
    private LocalDate endTime;
    private String reason;
    private Integer passengers;
    private String destination;
    private Integer vehicleType;
    private String vehicleTypeName;
    private String attachment;
    
    private Long applicantId;
    private String applicantName;
    private Integer deptId;
    private String deptName;
    private Integer targetDeptId;      // 新增
    private String targetDeptName;     // 新增
    
    private Integer templateId;
    private String templateName;
    private Integer templateType;      // 新增：1内部用车 2跨部门用车 3长途用车
    
    private Integer currentNode;
    private String currentNodeName;
    private List<String> currentApproverNames;
    
    private Integer status;
    private String statusName;
    private Integer isUrgent;
    
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}