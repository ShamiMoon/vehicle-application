package com.baoying.vehicleapplication.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 消息列表项响应
 */
@Data
public class MessageListItem {
    private Long id;
    
    private String title;             // 消息标题
    
    private String content;           // 消息内容
    
    private Integer messageType;      // 消息类型
    
    private String messageTypeName;   // 消息类型名称
    
    private Long applyId;             // 关联的申请ID
    
    private String applyTitle;        // 申请标题（方便展示）
    
    private Integer isRead;           // 是否已读
    
    private LocalDateTime createTime; // 创建时间
}
