package com.baoying.vehicleapplication.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class NodeApproverResponse {
    private Integer nodeOrder;
    private String nodeName;
    private List<ApproverInfo> approvers;
    
    @Data
    public static class ApproverInfo {
        private Long userId;
        private String realname;
        private Integer status;
    }
}