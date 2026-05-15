package com.baoying.vehicleapplication.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class ApproverNodeResponse {
    private Integer nodeOrder;
    private String nodeName;
    private String approverType;  // role / user / mixed
    private List<Long> userIds;    // 实际审批人用户ID列表
}