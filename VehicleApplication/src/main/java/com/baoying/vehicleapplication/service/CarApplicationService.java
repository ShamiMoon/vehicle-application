package com.baoying.vehicleapplication.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baoying.vehicleapplication.dto.request.CarApplyQueryRequest;
import com.baoying.vehicleapplication.dto.request.CarApplySaveRequest;
import com.baoying.vehicleapplication.dto.request.CarApplySubmitRequest;
import com.baoying.vehicleapplication.dto.request.CarApplyUpdateRequest;
import com.baoying.vehicleapplication.dto.response.CarApplyDetailResponse;
import com.baoying.vehicleapplication.dto.response.CarApplyListItem;
import com.baoying.vehicleapplication.entity.CarApplication;
import com.baoying.vehicleapplication.entity.ProcessApprover;

import java.util.List;

public interface CarApplicationService extends IService<CarApplication> {

    Long saveApplication(CarApplySaveRequest request,Long userId, Integer deptId);

    void submit(Long applyId,Long userId);
    /**
     * 提交用车申请
     */
    Long submitApplication(CarApplySubmitRequest request, Long userId, Integer deptId);

    /**
     * 修改用车申请（仅待提交/已驳回状态可修改）
     */
    void updateApplication(CarApplyUpdateRequest request, Long userId);

    /**
     * 撤销用车申请（仅待审批/审批中状态可撤销）
     */
    void cancelApplication(Long applyId, Long userId);

    /**
     * 获取个人申请列表（分页）
     */
    Page<CarApplyListItem> getMyApplicationList(CarApplyQueryRequest request, Long userId);

    /**
     * 获取全量申请列表（管理员，分页）
     * @param userId 当前用户ID，用于数据权限过滤
     */
    Page<CarApplyListItem> getAllApplicationList(CarApplyQueryRequest request, Long userId);

    /**
     * 获取申请详情
     */
    CarApplyDetailResponse getApplicationDetail(Long applyId);

    /**
     * 获取待审批列表
     */
    List<CarApplyListItem> getPendingList(Long userId);

    List<Long> getNodeApprovers(Integer templateId, Integer nodeOrder,
                                Integer applyDeptId, Integer targetDeptId);
    
    /**
     * 获取我审批过的申请列表（分页）
     */
    Page<CarApplyListItem> getApprovedByMeList(CarApplyQueryRequest request, Long userId);
    
    /**
     * 导出用车申请数据（Excel）
     */
    byte[] exportApplications(CarApplyQueryRequest request, Long userId);
    
    /**
     * 处理异常申请（管理员强制通过或驳回）
     * @param applyId 申请ID
     * @param action 操作：1-强制通过，2-强制驳回
     * @param reason 原因（驳回时必填）
     * @param operatorId 操作人ID
     */
    void handleAbnormalApplication(Long applyId, Integer action, String reason, Long operatorId);
}