package com.baoying.vehicleapplication.dto.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CarApplySaveRequest {
    private String title;
    private LocalDate startTime;
    private LocalDate endTime;
    private String reason;
    private Integer passengers;
    private String destination;
    private Integer vehicleType;
    private String attachment;
    private Integer templateId;
    private Integer isUrgent;
    private Integer targetDeptId;
}