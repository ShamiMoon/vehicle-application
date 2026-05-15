package com.baoying.vehicleapplication.dto.request;

import lombok.Data;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class ApproverUpdateRequest {
    @NotNull(message = "模板ID不能为空")
    private Integer templateId;

    @NotNull(message = "节点顺序不能为空")
    private Integer nodeOrder;

    /**
     * 审批人类型：role / user / mixed
     */
    @NotNull(message = "审批人类型不能为空")
    private String approverType;

    @NotNull(message = "审批人值不能为空")
    private List<Long> approverValue;
}