package com.baoying.vehicleapplication.dto.request;

import lombok.Data;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class ApproverAssignBatchRequest {
    @NotNull(message = "分配列表不能为空")
    private List<ApproverAssignRequest> items;
}