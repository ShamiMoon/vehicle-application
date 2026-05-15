package com.baoying.vehicleapplication.service;

import com.baoying.vehicleapplication.dto.request.MessageQueryRequest;
import com.baoying.vehicleapplication.dto.response.MessageListItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 消息服务接口
 */
public interface MessageService {
    
    /**
     * 发送系统消息
     * @param userId 接收用户ID
     * @param title 消息标题
     * @param content 消息内容
     * @param messageType 消息类型：1-待审批提醒 2-审批通过 3-审批驳回 4-转审通知 5-审批超时
     * @param applyId 关联的申请ID
     */
    void sendMessage(Long userId, String title, String content, Integer messageType, Long applyId);
    
    /**
     * 获取用户的消息列表（分页）
     * @param userId 用户ID
     * @param request 查询请求
     * @return 分页消息列表
     */
    Page<MessageListItem> getUserMessages(Long userId, MessageQueryRequest request);
    
    /**
     * 标记消息为已读
     * @param messageId 消息ID
     * @param userId 用户ID（用于权限校验）
     */
    void markAsRead(Long messageId, Long userId);
    
    /**
     * 标记所有消息为已读
     * @param userId 用户ID
     */
    void markAllAsRead(Long userId);
    
    /**
     * 获取未读消息数量
     * @param userId 用户ID
     * @return 未读消息数量
     */
    Integer getUnreadCount(Long userId);
}
