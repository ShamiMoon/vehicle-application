package com.baoying.vehicleapplication.controller;

import com.baoying.vehicleapplication.annotation.RequirePermission;
import com.baoying.vehicleapplication.common.Result;
import com.baoying.vehicleapplication.dto.request.MessageQueryRequest;
import com.baoying.vehicleapplication.dto.response.MessageListItem;
import com.baoying.vehicleapplication.service.MessageService;
import com.baoying.vehicleapplication.utils.CurrentUserUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息中心控制器
 */
@RestController
@RequestMapping("/msg")
@RequiredArgsConstructor
public class MessageController {
    
    private final MessageService messageService;
    
    /**
     * 获取我的消息列表（分页）
     */
    @RequirePermission(checkRole = false)
    @GetMapping("/list")
    public Result<Page<MessageListItem>> getMyMessages(MessageQueryRequest request) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        return Result.success(messageService.getUserMessages(userId, request));
    }
    
    /**
     * 标记消息为已读
     */
    @RequirePermission(checkRole = false, dataScope = "self")
    @PutMapping("/read/{messageId}")
    public Result<Void> markAsRead(@PathVariable Long messageId) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        messageService.markAsRead(messageId, userId);
        return Result.success();
    }
    
    /**
     * 标记所有消息为已读
     */
    @RequirePermission(checkRole = false, dataScope = "self")
    @PutMapping("/read-all")
    public Result<Void> markAllAsRead() {
        Long userId = CurrentUserUtils.getCurrentUserId();
        messageService.markAllAsRead(userId);
        return Result.success();
    }
    
    /**
     * 获取未读消息数量
     */
    @RequirePermission(checkRole = false)
    @GetMapping("/unread-count")
    public Result<Integer> getUnreadCount() {
        Long userId = CurrentUserUtils.getCurrentUserId();
        Integer count = messageService.getUnreadCount(userId);

        
        return Result.success(count);
    }
}
