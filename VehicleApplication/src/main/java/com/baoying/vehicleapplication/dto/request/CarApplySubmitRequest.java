package com.baoying.vehicleapplication.dto.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class CarApplySubmitRequest {

    @NotBlank(message = "申请标题不能为空")
    private String title;

    @NotNull(message = "用车开始日期不能为空")
    private LocalDate startTime;

    @NotNull(message = "用车结束日期不能为空")
    private LocalDate endTime;

    @NotBlank(message = "用车事由不能为空")
    private String reason;

    @NotNull(message = "用车人数不能为空")
    private Integer passengers;

    @NotBlank(message = "目的地不能为空")
    private String destination;

    @NotNull(message = "车辆类型不能为空")
    private Integer vehicleType;

    private String attachment;

    @NotNull(message = "流程模板ID不能为空")
    private Integer templateId;

    private Integer isUrgent;

    private Integer targetDeptId;
}