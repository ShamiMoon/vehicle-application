package com.baoying.vehicleapplication.dto.request;

import lombok.Data;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class ApproverAssignRequest {
    @NotNull(message = "模板ID不能为空")
    private Integer templateId;

    @NotNull(message = "节点顺序不能为空")
    private Integer nodeOrder;

    @NotNull(message = "审批人列表不能为空")
    private List<Long> userIds;

}