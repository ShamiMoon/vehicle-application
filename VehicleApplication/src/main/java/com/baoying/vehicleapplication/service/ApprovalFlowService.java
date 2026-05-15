package com.baoying.vehicleapplication.service;

import com.baoying.vehicleapplication.dto.request.ApprovalAgreeRequest;
import com.baoying.vehicleapplication.dto.request.ApprovalRejectRequest;
import com.baoying.vehicleapplication.dto.request.ApprovalTransferRequest;

public interface ApprovalFlowService {

    /**
     * 审批同意
     */
    void agree(ApprovalAgreeRequest request, Long approverId);

    /**
     * 审批驳回
     */
    void reject(ApprovalRejectRequest request, Long approverId);

    /**
     * 审批转审
     */
    void transfer(ApprovalTransferRequest request, Long approverId);
}