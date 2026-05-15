package com.baoying.vehicleapplication.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baoying.vehicleapplication.dto.request.ApproverAssignBatchRequest;
import com.baoying.vehicleapplication.dto.request.ApproverAssignRequest;
import com.baoying.vehicleapplication.dto.request.ApproverUpdateRequest;
import com.baoying.vehicleapplication.dto.response.ApproverNodeResponse;
import com.baoying.vehicleapplication.entity.ProcessApprover;

import java.util.List;

public interface ProcessApproverService extends IService<ProcessApprover> {

    /**
     * 为模板预置审批人（模板创建/编辑时调用）
     * @param templateId 模板ID
     * @param nodeConfig 节点配置列表
     */
    void preAssignApprovers(Integer templateId, List<?> nodeConfig);

    /**
     * 手动分配单个节点的审批人
     */
    void assignApprovers(ApproverAssignRequest request);

    void updateApprovers(ApproverUpdateRequest request);
    /**
     * 批量分配多个节点的审批人
     */
    void batchAssignApprovers(ApproverAssignBatchRequest request);

    /**
     * 删除模板某个节点的审批人
     */
    void deleteApprover(Integer templateId, Integer nodeOrder, Long userId);

    /**
     * 删除模板某个节点的所有审批人
     */
    void deleteAllApproversOfNode(Integer templateId, Integer nodeOrder);

    /**
     * 删除模板的所有审批人
     */
    void deleteAllApproversOfTemplate(Integer templateId);

    /**
     * 获取模板某个节点的所有预置审批人ID列表
     */
    List<Long> getApproverUserIds(Integer templateId, Integer nodeOrder);

    /**
     * 获取模板所有节点的审批人（按节点分组）
     */
    List<ApproverNodeResponse> getApproversByTemplate(Integer templateId);

    /**
     * 检查用户是否是模板节点的预置审批人
     */
    boolean isApprover(Integer templateId, Integer nodeOrder, Long userId);
}