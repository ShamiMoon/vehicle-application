package com.baoying.vehicleapplication.dto.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CarApplyUpdateRequest {

    @NotNull(message = "申请ID不能为空")
    private Long id;

    private String title;
    private LocalDate startTime;
    private LocalDate endTime;
    private String reason;
    private Integer passengers;
    private String destination;
    private Integer vehicleType;
    private Integer templateId;
    private String attachment;
    private Integer isUrgent;
    private Integer targetDeptId;
}