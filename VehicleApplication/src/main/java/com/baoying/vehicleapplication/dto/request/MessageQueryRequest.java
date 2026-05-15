package com.baoying.vehicleapplication.dto.request;

import lombok.Data;

/**
 * 消息列表查询请求
 */
@Data
public class MessageQueryRequest {
    
    /**
     * 是否已读（null-全部，0-未读，1-已读）
     */
    private Integer isRead;
    
    /**
     * 消息类型（可选）
     */
    private String messageType;
    
    /**
     * 页码（默认1）
     */
    private Integer pageNum = 1;
    
    /**
     * 每页大小（默认10）
     */
    private Integer pageSize = 10;
}
