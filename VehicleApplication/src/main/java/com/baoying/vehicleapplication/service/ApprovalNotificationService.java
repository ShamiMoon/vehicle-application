package com.baoying.vehicleapplication.service;

/**
 * 审批通知服务接口
 */
public interface ApprovalNotificationService {
    
    /**
     * 发送待审批提醒（给审批人）
     * @param approverId 审批人ID
     * @param applyTitle 申请标题
     * @param applyId 申请ID
     */
    void sendPendingApprovalNotification(Long approverId, String applyTitle, Long applyId);
    
    /**
     * 发送审批通过通知（给申请人）
     * @param applicantId 申请人ID
     * @param applyTitle 申请标题
     * @param applyId 申请ID
     */
    void sendApprovalPassedNotification(Long applicantId, String applyTitle, Long applyId);
    
    /**
     * 发送审批驳回通知（给申请人）
     * @param applicantId 申请人ID
     * @param applyTitle 申请标题
     * @param rejectReason 驳回原因
     * @param applyId 申请ID
     */
    void sendApprovalRejectedNotification(Long applicantId, String applyTitle, String rejectReason, Long applyId);
    
    /**
     * 发送转审通知（给转审目标人）
     * @param targetUserId 转审目标人ID
     * @param fromUserName 原审批人姓名
     * @param applyTitle 申请标题
     * @param applyId 申请ID
     */
    void sendTransferNotification(String targetUserId, String fromUserName, String applyTitle, Long applyId);
    void sendBackNotification(Long approverId, String fromUserName, String applyTitle, Long applyId);
    /**
     * 发送审批超时提醒（给审批人）
     * @param approverId 审批人ID
     * @param applyTitle 申请标题
     * @param applyId 申请ID
     */
    void sendTimeoutNotification(Long approverId, String applyTitle, Long applyId);
}
