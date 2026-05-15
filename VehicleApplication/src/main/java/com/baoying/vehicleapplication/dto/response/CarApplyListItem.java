package com.baoying.vehicleapplication.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CarApplyListItem {
    private Long id;
    private String title;
    private Integer status;
    private String statusName;
    private Integer isUrgent;
    private Integer currentNode;
    private String currentNodeName;
    private String currentApproverNames;
    private String applicantName;
    private String deptName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}