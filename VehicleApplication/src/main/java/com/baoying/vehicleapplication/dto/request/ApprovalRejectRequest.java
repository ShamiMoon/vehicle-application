package com.baoying.vehicleapplication.dto.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class ApprovalRejectRequest {
    @NotNull(message = "申请ID不能为空")
    private Long applyId;
    
    @NotBlank(message = "驳回原因不能为空")
    private String reason;
}