package com.baoying.vehicleapplication.service;

/**
 * 邮件服务接口
 */
public interface EmailService {
    
    /**
     * 发送审批提醒邮件
     * @param toEmail 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容（HTML格式）
     */
    void sendApprovalEmail(String toEmail, String subject, String content);
    
    /**
     * 构建审批通知邮件内容
     * @param userName 用户姓名
     * @param applyTitle 申请标题
     * @param status 审批状态
     * @param opinion 审批意见
     * @param applyId 申请ID
     * @return HTML邮件内容
     */
    String buildApprovalEmailContent(String userName, String applyTitle, String status, 
                                     String opinion, Long applyId);
}
