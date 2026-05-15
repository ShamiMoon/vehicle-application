package com.baoying.vehicleapplication.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baoying.vehicleapplication.entity.SysUser;
import com.baoying.vehicleapplication.mapper.UserMapper;
import com.baoying.vehicleapplication.service.ApprovalNotificationService;
import com.baoying.vehicleapplication.service.EmailService;
import com.baoying.vehicleapplication.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 审批通知服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalNotificationServiceImpl implements ApprovalNotificationService {
    
    private final MessageService messageService;
    private final EmailService emailService;
    private final UserMapper userMapper;
    
    @Override
    public void sendPendingApprovalNotification(Long approverId, String applyTitle, Long applyId) {
        SysUser approver = userMapper.selectById(approverId);
        if (approver == null) {
            log.warn("审批人不存在，userId: {}", approverId);
            return;
        }
        
        String title = "新的用车申请待审批";
        String content = "您有一笔用车申请需要审批：【" + applyTitle + "】";
        
        // 1. 发送系统消息（默认开启，不可关闭）
        messageService.sendMessage(approverId, title, content, 1, applyId);
        
        // 2. 发送邮件（根据用户配置）
        sendEmailIfEnabled(approver, title, content, applyId);
        
        log.info("已发送待审批提醒 - 审批人: {}, 申请: {}", approver.getRealname(), applyTitle);
    }
    
    @Override
    public void sendApprovalPassedNotification(Long applicantId, String applyTitle, Long applyId) {
        SysUser applicant = userMapper.selectById(applicantId);
        if (applicant == null) {
            log.warn("申请人不存在，userId: {}", applicantId);
            return;
        }
        
        String title = "用车申请已通过";
        String content = "您的用车申请【" + applyTitle + "】已审批通过";
        
        // 1. 发送系统消息
        messageService.sendMessage(applicantId, title, content, 2, applyId);
        
        // 2. 发送邮件
        sendEmailIfEnabled(applicant, title, content, applyId);
        
        log.info("已发送审批通过通知 - 申请人: {}, 申请: {}", applicant.getRealname(), applyTitle);
    }
    
    @Override
    public void sendApprovalRejectedNotification(Long applicantId, String applyTitle, String rejectReason, Long applyId) {
        SysUser applicant = userMapper.selectById(applicantId);
        if (applicant == null) {
            log.warn("申请人不存在，userId: {}", applicantId);
            return;
        }
        
        String title = "用车申请已驳回";
        String content = "您的用车申请【" + applyTitle + "】已被驳回，驳回原因：" + rejectReason;
        
        // 1. 发送系统消息
        messageService.sendMessage(applicantId, title, content, 3, applyId);
        
        // 2. 发送邮件
        sendEmailIfEnabled(applicant, title, content, applyId);
        
        log.info("已发送审批驳回通知 - 申请人: {}, 申请: {}", applicant.getRealname(), applyTitle);
    }

    @Override
    public void sendTransferNotification(String targetUserId, String fromUserName, String applyTitle, Long applyId) {
        // 解析多个目标用户ID（逗号分隔）
        if (StrUtil.isBlank(targetUserId)) {
            log.warn("转审目标人为空");
            return;
        }

        String[] userIds = targetUserId.split(",");
        for (String userIdStr : userIds) {
            if (StrUtil.isBlank(userIdStr)) {
                continue;
            }

            try {
                Long userId = Long.parseLong(userIdStr.trim());
                SysUser targetUser = userMapper.selectById(userId);
                if (targetUser == null) {
                    log.warn("转审目标人不存在，userId: {}", userId);
                    continue;
                }

                String title = "用车申请转审";
                String content = fromUserName + " 将用车申请【" + applyTitle + "】转交给您审批";

                // 1. 发送系统消息
                messageService.sendMessage(userId, title, content, 4, applyId);

                // 2. 发送邮件
                sendEmailIfEnabled(targetUser, title, content, applyId);

                log.info("已发送转审通知 - 目标人: {}, 申请: {}", targetUser.getRealname(), applyTitle);
            } catch (NumberFormatException e) {
                log.error("转审目标人ID格式错误: {}", userIdStr, e);
            }
        }
    }
    @Override
    public void sendBackNotification(Long approverId, String fromUserName, String applyTitle, Long applyId) {
        SysUser approver = userMapper.selectById(approverId);
        if (approver == null) {
            log.warn("原审批人不存在，userId: {}", approverId);
            return;
        }

        String title = "转审操作反馈";
        String content = "您已将用车申请【" + applyTitle + "】转交给 " + fromUserName + " 审批";

        // 1. 发送系统消息
        messageService.sendMessage(approverId, title, content, 6, applyId);

        // 2. 发送邮件（根据用户配置）
        sendEmailIfEnabled(approver, title, content, applyId);

        log.info("已发送转审反馈通知 - 原审批人: {}, 申请: {}", approver.getRealname(), applyTitle);
    }

    @Override
    public void sendTimeoutNotification(Long approverId, String applyTitle, Long applyId) {
        SysUser approver = userMapper.selectById(approverId);
        if (approver == null) {
            log.warn("审批人不存在，userId: {}", approverId);
            return;
        }
        
        String title = "审批超时提醒";
        String content = "您有一笔用车申请【" + applyTitle + "】尚未处理，请尽快审批";
        
        // 1. 发送系统消息
        messageService.sendMessage(approverId, title, content, 5, applyId);
        
        // 2. 发送邮件
        sendEmailIfEnabled(approver, title, content, applyId);
        
        log.info("已发送审批超时提醒 - 审批人: {}, 申请: {}", approver.getRealname(), applyTitle);
    }
    
    /**
     * 如果用户开启了邮件通知，则发送邮件
     */
    private void sendEmailIfEnabled(SysUser user, String title, String content, Long applyId) {
        // 检查用户是否开启了邮件通知
        if (user.getEmailNotify() == null || user.getEmailNotify() != 1) {
            log.debug("用户 {} 未开启邮件通知", user.getRealname());
            return;
        }
        
        // 检查用户是否有邮箱
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            log.warn("用户 {} 未绑定邮箱", user.getRealname());
            return;
        }
        
        // 构建并发送邮件
        String emailContent = emailService.buildApprovalEmailContent(
            user.getRealname(), 
            content, 
            getMessageStatusName(getMessageTypeFromTitle(title)),
            null,
            applyId
        );
        
        emailService.sendApprovalEmail(user.getEmail(), title, emailContent);
    }
    
    /**
     * 根据标题获取消息类型
     */
    private Integer getMessageTypeFromTitle(String title) {
        if (title.contains("待审批")) {
            return 1;
        } else if (title.contains("通过")) {
            return 2;
        } else if (title.contains("驳回")) {
            return 3;
        } else if (title.contains("转审")) {
            return 4;
        } else if (title.contains("超时")) {
            return 5;
        }
        return 1;
    }
    
    /**
     * 获取消息状态名称
     */
    private String getMessageStatusName(Integer messageType) {
        if (messageType == null) {
            return "未知";
        }
        switch (messageType) {
            case 1: return "待审批提醒";
            case 2: return "审批通过";
            case 3: return "审批驳回";
            case 4: return "转审通知";
            case 5: return "审批超时";
            default: return "未知";
        }
    }
}
