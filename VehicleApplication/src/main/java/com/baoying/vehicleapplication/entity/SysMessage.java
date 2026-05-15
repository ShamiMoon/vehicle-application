package com.baoying.vehicleapplication.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统消息实体
 */
@Data
@TableName("sys_message")
public class SysMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;              // 接收用户ID
    
    private String title;             // 消息标题
    
    private String content;           // 消息内容
    
    private Integer messageType;      // 消息类型：1-待审批提醒 2-审批通过 3-审批驳回 4-转审通知 5-审批超时
    
    private Long applyId;             // 关联的申请ID
    
    private Integer isRead;           // 是否已读：0-未读 1-已读
    
    private LocalDateTime readTime;   // 阅读时间
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime; // 创建时间
}
