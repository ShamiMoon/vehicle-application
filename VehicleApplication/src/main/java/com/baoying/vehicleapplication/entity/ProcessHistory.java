package com.baoying.vehicleapplication.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;

import java.time.LocalDateTime;

@Data
@TableName("process_history")
public class ProcessHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long applyId;
    private Integer nodeOrder;
    private String nodeName;
    private Long processBy;
    private Integer action;  // 1同意 2驳回 3转审
    private String opinion;
    @TableField(jdbcType = JdbcType.VARCHAR)
    private String transferTo;
    private LocalDateTime processTime;
}