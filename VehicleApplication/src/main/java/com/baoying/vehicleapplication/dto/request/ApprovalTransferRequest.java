package com.baoying.vehicleapplication.dto.request;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class ApprovalTransferRequest {
    @NotNull(message = "申请ID不能为空")
    private Long applyId;
    
    @NotNull(message = "转审目标用户ID不能为空")
    private String transferTo;
    
    private String opinion;
}