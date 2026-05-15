package com.baoying.vehicleapplication.service;

import com.baoying.vehicleapplication.dto.response.MessageListItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 消息服务测试
 */
@SpringBootTest
public class MessageServiceTest {
    
    @Autowired
    private MessageService messageService;
    
    @Test
    public void testSendMessage() {
        // 发送测试消息
        Long testUserId = 1L;
        messageService.sendMessage(
            testUserId,
            "测试消息",
            "这是一条测试消息",
            1,
            null
        );
        
        // 验证消息已发送
        List<MessageListItem> messages = messageService.getUserMessages(testUserId, null);
        assertFalse(messages.isEmpty(), "消息列表不应为空");
    }
    
    @Test
    public void testGetUserMessages() {
        Long testUserId = 1L;
        
        // 获取所有消息
        List<MessageListItem> allMessages = messageService.getUserMessages(testUserId, null);
        assertNotNull(allMessages);
        
        // 获取未读消息
        List<MessageListItem> unreadMessages = messageService.getUserMessages(testUserId, 0);
        assertNotNull(unreadMessages);
    }
    
    @Test
    public void testMarkAsRead() {
        Long testUserId = 1L;
        
        // 先发送一条消息
        messageService.sendMessage(testUserId, "测试", "内容", 1, null);
        
        // 获取未读消息
        List<MessageListItem> unreadMessages = messageService.getUserMessages(testUserId, 0);
        if (!unreadMessages.isEmpty()) {
            Long messageId = unreadMessages.get(0).getId();
            
            // 标记为已读
            messageService.markAsRead(messageId, testUserId);
            
            // 验证已读
            List<MessageListItem> updatedUnread = messageService.getUserMessages(testUserId, 0);
            assertTrue(updatedUnread.stream().noneMatch(m -> m.getId().equals(messageId)));
        }
    }
    
    @Test
    public void testGetUnreadCount() {
        Long testUserId = 1L;
        
        Integer count = messageService.getUnreadCount(testUserId);
        assertNotNull(count);
        assertTrue(count >= 0);
    }
}
