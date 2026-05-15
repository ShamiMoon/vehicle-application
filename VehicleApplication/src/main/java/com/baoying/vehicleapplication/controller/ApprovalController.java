package com.baoying.vehicleapplication.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baoying.vehicleapplication.common.Result;
import com.baoying.vehicleapplication.dto.request.ApprovalAgreeRequest;
import com.baoying.vehicleapplication.dto.request.ApprovalRejectRequest;
import com.baoying.vehicleapplication.dto.request.ApprovalTransferRequest;
import com.baoying.vehicleapplication.entity.ProcessHistory;
import com.baoying.vehicleapplication.mapper.ProcessHistoryMapper;
import com.baoying.vehicleapplication.service.ApprovalFlowService;
import com.baoying.vehicleapplication.utils.CurrentUserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/apply/app")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalFlowService approvalFlowService;
    @Autowired
    private ProcessHistoryMapper historyMapper;

    /**
     * 审批同意
     */
    @PostMapping("/agree")
    public Result<Void> agree(@Valid @RequestBody ApprovalAgreeRequest request) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        approvalFlowService.agree(request, userId);
        return Result.success();
    }

    /**
     * 审批驳回
     */
    @PostMapping("/reject")
    public Result<Void> reject(@Valid @RequestBody ApprovalRejectRequest request) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        approvalFlowService.reject(request, userId);
        return Result.success();
    }

    /**
     * 审批转审
     */
    @PostMapping("/transfer")
    public Result<Void> transfer(@Valid @RequestBody ApprovalTransferRequest request) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        approvalFlowService.transfer(request, userId);
        return Result.success();
    }
    /**
     * 获取某个申请的审批历史
     */
    @GetMapping("/history/{applyId}")
    public Result<List<ProcessHistory>> history(@PathVariable Long applyId) {
        LambdaQueryWrapper<ProcessHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessHistory::getApplyId, applyId)
                .orderByAsc(ProcessHistory::getProcessTime);
        return Result.success(historyMapper.selectList(wrapper));
    }
}