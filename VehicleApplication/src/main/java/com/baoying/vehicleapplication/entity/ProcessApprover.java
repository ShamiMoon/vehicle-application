package com.baoying.vehicleapplication.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("process_approver")
public class ProcessApprover {
    private Integer templateId;
    private Integer nodeOrder;
    private Long userId;
}