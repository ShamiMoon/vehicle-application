package com.baoying.vehicleapplication.controller;

import com.baoying.vehicleapplication.annotation.RequirePermission;
import com.baoying.vehicleapplication.common.Result;
import com.baoying.vehicleapplication.dto.request.ApproverAssignBatchRequest;
import com.baoying.vehicleapplication.dto.request.ApproverAssignRequest;
import com.baoying.vehicleapplication.dto.request.ApproverUpdateRequest;
import com.baoying.vehicleapplication.dto.response.ApproverNodeResponse;
import com.baoying.vehicleapplication.service.ProcessApproverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/flow/approver")
public class ProcessApproverController {

    @Autowired
    private ProcessApproverService approverService;

    /**
     * 手动分配单个节点的审批人
     */
    @PostMapping("/assign")
    public Result<Void> assign(@Valid @RequestBody ApproverAssignRequest request) {
        approverService.assignApprovers(request);
        return Result.success();
    }

    /**
     * 批量分配多个节点的审批人
     */
    @PostMapping("/batch-assign")
    public Result<Void> batchAssign(@Valid @RequestBody ApproverAssignBatchRequest request) {
        approverService.batchAssignApprovers(request);
        return Result.success();
    }

    /**
     * 针对某个修改分配审批人
     */
    @PutMapping("/update")
    public Result<Void> update(@Valid @RequestBody ApproverUpdateRequest request) {
        approverService.updateApprovers(request);
        return Result.success();
    }

    /**
     * 删除模板某个节点的审批人
     */
    @RequirePermission(roles = {1})
    @DeleteMapping("/delete")
    public Result<Void> delete(@RequestParam Integer templateId,
                                @RequestParam Integer nodeOrder,
                                @RequestParam Long userId) {
        approverService.deleteApprover(templateId, nodeOrder, userId);
        return Result.success();
    }

    /**
     * 删除模板某个节点的所有审批人
     */
    @RequirePermission(roles = {1})
    @DeleteMapping("/delete-all/{templateId}/{nodeOrder}")
    public Result<Void> deleteAllOfNode(@PathVariable Integer templateId,
                                         @PathVariable Integer nodeOrder) {
        approverService.deleteAllApproversOfNode(templateId, nodeOrder);
        return Result.success();
    }

    /**
     * 删除模板的所有审批人
     */
    @RequirePermission(roles = {1})
    @DeleteMapping("/delete-all/{templateId}")
    public Result<Void> deleteAllOfTemplate(@PathVariable Integer templateId) {
        approverService.deleteAllApproversOfTemplate(templateId);
        return Result.success();
    }

    /**
     * 获取模板某个节点的预置审批人ID列表
     */
    @GetMapping("/user-ids/{templateId}/{nodeOrder}")
    public Result<List<Long>> getUserIds(@PathVariable Integer templateId,
                                         @PathVariable Integer nodeOrder) {
        return Result.success(approverService.getApproverUserIds(templateId, nodeOrder));
    }

    /**
     * 获取模板所有节点的审批人（按节点分组）
     */
    @GetMapping("/list/{templateId}")
    public Result<List<ApproverNodeResponse>> list(@PathVariable Integer templateId) {
        return Result.success(approverService.getApproversByTemplate(templateId));
    }
}