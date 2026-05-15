package com.baoying.vehicleapplication.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baoying.vehicleapplication.common.BusinessException;
import com.baoying.vehicleapplication.dto.request.MessageQueryRequest;
import com.baoying.vehicleapplication.dto.response.MessageListItem;
import com.baoying.vehicleapplication.entity.CarApplication;
import com.baoying.vehicleapplication.entity.SysMessage;
import com.baoying.vehicleapplication.mapper.CarApplicationMapper;
import com.baoying.vehicleapplication.mapper.SysMessageMapper;
import com.baoying.vehicleapplication.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息服务实现
 */
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {
    
    private final SysMessageMapper messageMapper;
    private final CarApplicationMapper carApplicationMapper;
    
    @Override
    public void sendMessage(Long userId, String title, String content, Integer messageType, Long applyId) {
        SysMessage message = new SysMessage();
        message.setUserId(userId);
        message.setTitle(title);
        message.setContent(content);
        message.setMessageType(messageType);
        message.setApplyId(applyId);
        message.setIsRead(0);  // 未读
        message.setCreateTime(LocalDateTime.now());
        
        messageMapper.insert(message);
    }
    
    @Override
    public Page<MessageListItem> getUserMessages(Long userId, MessageQueryRequest request) {
        // 构建查询条件
        LambdaQueryWrapper<SysMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysMessage::getUserId, userId);
        
        // 是否已读筛选
        if (request.getIsRead() != null) {
            queryWrapper.eq(SysMessage::getIsRead, request.getIsRead());
        }
        
        // 消息类型筛选
        if (request.getMessageType() != null && !request.getMessageType().trim().isEmpty()) {
            queryWrapper.eq(SysMessage::getMessageType, request.getMessageType());
        }
        
        queryWrapper.orderByDesc(SysMessage::getCreateTime);
        
        // 执行分页查询
        Page<SysMessage> messagePage = new Page<>(request.getPageNum(), request.getPageSize());
        messagePage = messageMapper.selectPage(messagePage, queryWrapper);
        
        // 转换为 Page<MessageListItem>
        Page<MessageListItem> resultPage = new Page<>(messagePage.getCurrent(), messagePage.getSize(), messagePage.getTotal());
        List<MessageListItem> itemList = messagePage.getRecords().stream().map(msg -> {
            MessageListItem item = new MessageListItem();
            item.setId(msg.getId());
            item.setTitle(msg.getTitle());
            item.setContent(msg.getContent());
            item.setMessageType(msg.getMessageType());
            item.setMessageTypeName(getMessageTypeName(msg.getMessageType()));
            item.setApplyId(msg.getApplyId());
            item.setIsRead(msg.getIsRead());
            item.setCreateTime(msg.getCreateTime());
            
            // 获取申请标题
            if (msg.getApplyId() != null) {
                CarApplication application = carApplicationMapper.selectById(msg.getApplyId());
                if (application != null) {
                    item.setApplyTitle(application.getTitle());
                }
            }
            
            return item;
        }).collect(Collectors.toList());
        
        resultPage.setRecords(itemList);
        return resultPage;
    }
    
    @Override
    public void markAsRead(Long messageId, Long userId) {
        SysMessage message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException("消息不存在");
        }
        
        if (!message.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此消息");
        }
        
        messageMapper.markAsRead(messageId);
    }
    
    @Override
    public void markAllAsRead(Long userId) {
        messageMapper.markAllAsRead(userId);
    }
    
    @Override
    public Integer getUnreadCount(Long userId) {
        LambdaQueryWrapper<SysMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMessage::getUserId, userId)
               .eq(SysMessage::getIsRead, 0);
        
        return Math.toIntExact(messageMapper.selectCount(wrapper));
    }
    
    /**
     * 获取消息类型名称
     */
    private String getMessageTypeName(Integer messageType) {
        if (messageType == null) {
            return "未知";
        }
        switch (messageType) {
            case 1: return "待审批提醒";
            case 2: return "审批通过";
            case 3: return "审批驳回";
            case 4: return "转审通知";
            case 5: return "审批超时";
            case 6: return "密码通知";
            case 7: return "密码重置申请";
            default: return "未知";
        }
    }
}
