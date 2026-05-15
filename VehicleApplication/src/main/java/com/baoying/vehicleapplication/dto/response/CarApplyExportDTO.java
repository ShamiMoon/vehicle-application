package com.baoying.vehicleapplication.dto.response;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用车申请导出DTO
 */
@Data
public class CarApplyExportDTO {
    
    @ExcelProperty(value = "申请ID", index = 0)
    private Long id;
    
    @ExcelProperty(value = "申请标题", index = 1)
    private String title;
    
    @ExcelProperty(value = "申请人", index = 2)
    private String applicantName;
    
    @ExcelProperty(value = "所属部门", index = 3)
    private String deptName;
    
    @ExcelProperty(value = "目标部门", index = 4)
    private String targetDeptName;
    
    @ExcelProperty(value = "用车开始日期", index = 5)
    @DateTimeFormat("yyyy-MM-dd")
    private LocalDate startTime;
    
    @ExcelProperty(value = "用车结束日期", index = 6)
    @DateTimeFormat("yyyy-MM-dd")
    private LocalDate endTime;
    
    @ExcelProperty(value = "用车事由", index = 7)
    private String reason;
    
    @ExcelProperty(value = "同行人数", index = 8)
    private Integer passengers;
    
    @ExcelProperty(value = "目的地", index = 9)
    private String destination;
    
    @ExcelProperty(value = "车辆类型", index = 10)
    private String vehicleTypeName;
    
    @ExcelProperty(value = "是否紧急", index = 11)
    private String isUrgentName;
    
    @ExcelProperty(value = "流程模板", index = 12)
    private String templateName;
    
    @ExcelProperty(value = "审批状态", index = 13)
    private String statusName;
    
    @ExcelProperty(value = "当前节点", index = 14)
    private String currentNodeName;
    
    @ExcelProperty(value = "当前审批人", index = 15)
    private String currentApproverNames;
    
    @ExcelProperty(value = "申请时间", index = 16)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    @ExcelProperty(value = "更新时间", index = 17)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
