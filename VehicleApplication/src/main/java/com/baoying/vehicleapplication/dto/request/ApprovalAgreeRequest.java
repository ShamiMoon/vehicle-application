package com.baoying.vehicleapplication.dto.request;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class ApprovalAgreeRequest {
    @NotNull(message = "申请ID不能为空")
    private Long applyId;
    
    private String opinion;  // 审批意见（可选）

    private String transferTo; // 如果有下一个节点，则指定
}