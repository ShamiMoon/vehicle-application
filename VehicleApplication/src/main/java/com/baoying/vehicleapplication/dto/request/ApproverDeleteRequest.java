package com.baoying.vehicleapplication.dto.request;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class ApproverDeleteRequest {
    @NotNull(message = "模板ID不能为空")
    private Integer templateId;
    
    @NotNull(message = "节点顺序不能为空")
    private Integer nodeOrder;
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
}