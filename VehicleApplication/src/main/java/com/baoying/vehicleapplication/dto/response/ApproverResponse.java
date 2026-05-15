package com.baoying.vehicleapplication.dto.response;

import lombok.Data;

@Data
public class ApproverResponse {
    private Integer templateId;
    private Integer nodeOrder;
    private String nodeName;      // 节点名称（从模板JSON中获取）
    private Long userId;
    private String userRealname;  // 审批人姓名
    private Integer status;
    private String statusName;    // 生效中/已失效
}