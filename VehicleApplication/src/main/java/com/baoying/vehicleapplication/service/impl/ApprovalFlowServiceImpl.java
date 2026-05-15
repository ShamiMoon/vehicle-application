package com.baoying.vehicleapplication.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baoying.vehicleapplication.common.ApplyStatusEnum;
import com.baoying.vehicleapplication.common.BusinessException;
import com.baoying.vehicleapplication.dto.request.ApprovalAgreeRequest;
import com.baoying.vehicleapplication.dto.request.ApprovalRejectRequest;
import com.baoying.vehicleapplication.dto.request.ApprovalTransferRequest;
import com.baoying.vehicleapplication.entity.*;
import com.baoying.vehicleapplication.mapper.*;
import com.baoying.vehicleapplication.service.ApprovalFlowService;
import com.baoying.vehicleapplication.service.CarApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalFlowServiceImpl implements ApprovalFlowService {

    private final CarApplicationMapper carApplicationMapper;
    private final ProcessTemplateMapper templateMapper;
    private final ProcessHistoryMapper historyMapper;
    private final UserMapper userMapper;
    private final CarApplicationService carApplicationService;
    private final com.baoying.vehicleapplication.service.ApprovalNotificationService notificationService;

    @Override
    @Transactional
    public void agree(ApprovalAgreeRequest request, Long approverId) {
        // 1. 查询申请
        CarApplication application = carApplicationMapper.selectById(request.getApplyId());
        if (application == null) {
            throw new BusinessException("申请不存在");
        }

        // 2. 校验状态（只有待审批或审批中才能审批）
        if (application.getStatus() != ApplyStatusEnum.PENDING.getCode() &&
            application.getStatus() != ApplyStatusEnum.PROCESSING.getCode()) {
            throw new BusinessException("当前状态不可审批");
        }

        // 3. 校验当前用户是否为当前节点的审批人
        List<Long> currentApprovers = parseCurrentApproverIds(application.getCurrentApproverIds());
        if (!currentApprovers.contains(approverId)) {
            throw new BusinessException("您不是当前节点的审批人");
        }

        // 4. 获取模板配置
        ProcessTemplate template = templateMapper.selectById(application.getTemplateId());
        JSONArray nodes = JSONUtil.parseArray(template.getNodeConfig());
        int totalNodes = nodes.size();

        // 5. 获取当前节点配置和名称
        JSONObject currentNodeConfig = getNodeConfig(nodes, application.getCurrentNode());
        String currentNodeName = getNodeName(nodes, application.getCurrentNode());
        String approveType = currentNodeConfig.getStr("approveType"); // single / all

        // 6. 检查是否已经审批过
        LambdaQueryWrapper<ProcessHistory> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(ProcessHistory::getApplyId, application.getId())
                   .eq(ProcessHistory::getNodeOrder, application.getCurrentNode())
                   .eq(ProcessHistory::getProcessBy, approverId)
                   .eq(ProcessHistory::getAction, 1); // 同意
        long approvedCount = historyMapper.selectCount(checkWrapper);
        if (approvedCount > 0) {
            throw new BusinessException("您已经审批过了，请勿重复操作");
        }

        // 7. 记录审批历史
        ProcessHistory history = new ProcessHistory();
        history.setApplyId(application.getId());
        history.setNodeOrder(application.getCurrentNode());
        history.setNodeName(currentNodeName);
        history.setProcessBy(approverId);
        history.setAction(1);  // 同意
        history.setOpinion(request.getOpinion());
        history.setProcessTime(LocalDateTime.now());
        historyMapper.insert(history);

        // 8. 根据审批方式判断是否进入下一节点
        if ("all".equals(approveType)) {
            // 会签：需要所有人都同意
            handleCountersign(application, nodes, totalNodes, currentApprovers, history);
        } else {
            // 或签（默认）：任意一人同意即可
            handleSingleSign(application, nodes, totalNodes, history);
        }
    }

    @Override
    @Transactional
    public void reject(ApprovalRejectRequest request, Long approverId) {
        // 1. 查询申请
        CarApplication application = carApplicationMapper.selectById(request.getApplyId());
        if (application == null) {
            throw new BusinessException("申请不存在");
        }

        // 2. 校验状态
        if (application.getStatus() != ApplyStatusEnum.PENDING.getCode() &&
            application.getStatus() != ApplyStatusEnum.PROCESSING.getCode()) {
            throw new BusinessException("当前状态不可审批");
        }

        // 3. 校验当前用户是否为当前节点的审批人
        List<Long> currentApprovers = parseCurrentApproverIds(application.getCurrentApproverIds());
        if (!currentApprovers.contains(approverId)) {
            throw new BusinessException("您不是当前节点的审批人");
        }

        // 4. 获取模板配置
        ProcessTemplate template = templateMapper.selectById(application.getTemplateId());
        JSONArray nodes = JSONUtil.parseArray(template.getNodeConfig());

        // 5. 获取当前节点配置和名称
        JSONObject currentNodeConfig = getNodeConfig(nodes, application.getCurrentNode());
        String currentNodeName = getNodeName(nodes, application.getCurrentNode());

        // 6. 记录审批历史
        ProcessHistory history = new ProcessHistory();
        history.setApplyId(application.getId());
        history.setNodeOrder(application.getCurrentNode());
        history.setNodeName(currentNodeName);
        history.setProcessBy(approverId);
        history.setAction(2);  // 驳回
        history.setOpinion(request.getReason());
        history.setProcessTime(LocalDateTime.now());
        historyMapper.insert(history);

        // 7. 获取当前节点配置中的驳回规则
        String rejectRule = currentNodeConfig != null ? currentNodeConfig.getStr("rejectRule", "return_to_start") : "return_to_start";

        if ("end".equals(rejectRule)) {
            // 终止驳回：申请人无法重新提交
            application.setStatus(ApplyStatusEnum.REJECTED_END.getCode());
        } else {
            // return_to_start（默认）：驳回后返回发起人，可修改后重新提交
            application.setStatus(ApplyStatusEnum.REJECTED.getCode());
        }
        application.setCurrentNode(null);
        application.setCurrentApproverIds(null);
        application.setUpdateTime(LocalDateTime.now());
        carApplicationMapper.updateById(application);

        // 8. 发送通知给申请人
        notificationService.sendApprovalRejectedNotification(
            application.getApplyBy(), 
            application.getTitle(),
            request.getReason(),
            application.getId()
        );
    }

    @Override
    @Transactional
    public void transfer(ApprovalTransferRequest request, Long approverId) {
        // 1. 查询申请
        CarApplication application = carApplicationMapper.selectById(request.getApplyId());
        if (application == null) {
            throw new BusinessException("申请不存在");
        }

        // 2. 校验状态
        if (application.getStatus() != ApplyStatusEnum.PENDING.getCode() &&
            application.getStatus() != ApplyStatusEnum.PROCESSING.getCode()) {
            throw new BusinessException("当前状态不可转审");
        }

        // 3. 校验当前用户是否为当前节点的审批人
        List<Long> currentApprovers = parseCurrentApproverIds(application.getCurrentApproverIds());
        if (!currentApprovers.contains(approverId)) {
            throw new BusinessException("您不是当前节点的审批人");
        }

        // 4. 校验目标用户是否存在且启用
        List<Long> transferToUserIds = parseCurrentApproverIds(request.getTransferTo());
        if (transferToUserIds.isEmpty()) {
            throw new BusinessException("转审目标用户不能为空");
        }

        for (Long userId : transferToUserIds) {
            SysUser targetUser = userMapper.selectById(userId);
            if (targetUser == null) {
                throw new BusinessException("转审目标用户不存在，ID: " + userId);
            }
            if (targetUser.getStatus() != 1) {
                throw new BusinessException("转审目标用户已被禁用，ID: " + userId);
            }
        }

        // 5. 获取模板配置和节点名称
        ProcessTemplate template = templateMapper.selectById(application.getTemplateId());
        JSONArray nodes = JSONUtil.parseArray(template.getNodeConfig());
        String currentNodeName = getNodeName(nodes, application.getCurrentNode());

        // 6. 记录审批历史（转审）
        ProcessHistory history = new ProcessHistory();
        history.setApplyId(application.getId());
        history.setNodeOrder(application.getCurrentNode());
        history.setNodeName(currentNodeName);
        history.setProcessBy(approverId);
        history.setAction(3);  // 转审
        history.setOpinion(request.getOpinion());

        history.setTransferTo(request.getTransferTo());
        history.setProcessTime(LocalDateTime.now());

        historyMapper.insert(history);

        // 7. 更新当前节点的审批人（替换为转审目标人）
        application.setCurrentApproverIds(request.getTransferTo());
        application.setUpdateTime(LocalDateTime.now());
        carApplicationMapper.updateById(application);


        // 8. 发送通知给转审目标人
        SysUser fromUser = userMapper.selectById(approverId);
        String fromUserName = fromUser != null ? fromUser.getRealname() : "未知用户";
        notificationService.sendTransferNotification(
                request.getTransferTo(),
                fromUserName,
                application.getTitle(),
                application.getId()
        );

        // 9. 发送通知给原审批人（系统消息）
        notificationService.sendBackNotification(
            approverId,
            "系统",
            "您已将用车申请【" + application.getTitle() + "】转审给其他审批人",
            application.getId()
        );
    }

    /**
     * 处理或签（任意一人同意即可）
     */
    private void handleSingleSign(CarApplication application, JSONArray nodes, int totalNodes, ProcessHistory history) {
        if (application.getCurrentNode() >= totalNodes) {
            // 审批完成
            completeApproval(application);
        } else {
            // 进入下一节点
            goToNextNode(application, nodes, history);
        }
    }

    /**
     * 处理会签（所有人都要同意）
     */
    private void handleCountersign(CarApplication application, JSONArray nodes, int totalNodes, 
                                   List<Long> currentApprovers, ProcessHistory history) {
        // 查询当前节点已同意的人数
        LambdaQueryWrapper<ProcessHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessHistory::getApplyId, application.getId())
               .eq(ProcessHistory::getNodeOrder, application.getCurrentNode())
               .eq(ProcessHistory::getAction, 1); // 同意
        long approvedCount = historyMapper.selectCount(wrapper);

        // 判断是否所有人都已同意
        if (approvedCount >= currentApprovers.size()) {
            // 所有人都同意了，进入下一节点或完成审批
            if (application.getCurrentNode() >= totalNodes) {
                completeApproval(application);
            } else {
                goToNextNode(application, nodes, history);
            }
        } else {
            // 还有人没审批，停留在当前节点
            log.info("会签节点：{}/{} 人已同意，等待其他人审批", approvedCount, currentApprovers.size());
            // 更新申请状态为审批中（如果还不是）
            if (!ApplyStatusEnum.PROCESSING.getCode().equals(application.getStatus())) {
                application.setStatus(ApplyStatusEnum.PROCESSING.getCode());
                application.setUpdateTime(LocalDateTime.now());
                carApplicationMapper.updateById(application);
            }
        }
    }

    /**
     * 完成审批
     */
    private void completeApproval(CarApplication application) {
        application.setStatus(ApplyStatusEnum.APPROVED.getCode());
        application.setCurrentNode(null);
        application.setCurrentApproverIds(null);
        application.setUpdateTime(LocalDateTime.now());
        carApplicationMapper.updateById(application);

        // 发送通知给申请人
        notificationService.sendApprovalPassedNotification(
            application.getApplyBy(), 
            application.getTitle(), 
            application.getId()
        );
    }

    /**
     * 进入下一节点（使用快照中的节点配置，不受模板编辑影响）
     */
    private void goToNextNode(CarApplication application, JSONArray nodes, ProcessHistory history) {
        // 优先使用申请提交时的模板快照，确保节点配置不受后续模板编辑影响
        String snapshot = application.getNodeConfigSnapshot();
        JSONArray nodeConfig = snapshot != null ? JSONUtil.parseArray(snapshot) : nodes;

        int nextNode = application.getCurrentNode() + 1;
        JSONObject nextNodeConfig = getNodeConfig(nodeConfig, nextNode);

        // 获取下一节点的审批人（传入申请人部门ID和目标部门ID）
        List<Long> nextApprovers = getApproversFromNodeConfig(
            nextNodeConfig, application.getDeptId(), application.getTargetDeptId()
        );

        if (nextApprovers.isEmpty()) {
            throw new BusinessException("下一节点未配置审批人，请联系管理员");
        }

        // 更新申请
        history.setTransferTo(StrUtil.join(",", nextApprovers));
        application.setCurrentNode(nextNode);
        application.setCurrentApproverIds(StrUtil.join(",", nextApprovers));
        application.setStatus(ApplyStatusEnum.PROCESSING.getCode());
        application.setUpdateTime(LocalDateTime.now());
        carApplicationMapper.updateById(application);

        // 发送通知给下一节点的审批人
        for (Long approver : nextApprovers) {
            notificationService.sendPendingApprovalNotification(
                approver, 
                application.getTitle(), 
                application.getId()
            );
        }
    }

    /**
     * 从节点配置JSON中解析审批人列表（不使用模板实时配置，保证已流转申请的审批人不变）
     */
    private List<Long> getApproversFromNodeConfig(JSONObject nodeConfig, Integer deptId, Integer targetDeptId) {
        String approverType = nodeConfig.getStr("approverType");
        Object approverValueObj = nodeConfig.get("approverValue");
        String dynamicType = nodeConfig.getStr("dynamicType");

        if ("target_dept".equals(dynamicType)) {
            if (targetDeptId == null) return Collections.emptyList();
            return getApproversByDeptAndRole(targetDeptId, approverType, approverValueObj);
        }

        Integer useDeptId = "applicant_dept".equals(dynamicType) ? deptId : null;
        return getApproversByConfig(approverType, approverValueObj, useDeptId);
    }

    /**
     * 根据部门和角色获取审批人
     */
    private List<Long> getApproversByDeptAndRole(Integer deptId, String approverType, Object approverValueObj) {
        List<Long> result = new ArrayList<>();
        List<Long> ids = parseApproverValues(approverValueObj);
        // 混合类型中包含角色ID和用户ID
        if ("role".equals(approverType)) {
            for (Long roleId : ids) {
                List<SysUser> users = userMapper.selectList(
                    new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRoleId, roleId)
                        .eq(SysUser::getDeptId, deptId)
                        .eq(SysUser::getStatus, 1)
                );
                for (SysUser u : users) result.add(u.getId());
            }
        } else if ("user".equals(approverType)) {
            result.addAll(ids);
        }
        return result;
    }

    /**
     * 根据配置类型解析审批人
     */
    private List<Long> getApproversByConfig(String approverType, Object approverValueObj, Integer deptId) {
        List<Long> result = new ArrayList<>();
        List<Long> values = parseApproverValues(approverValueObj);

        if ("role".equals(approverType)) {
            List<Long> roleIds = values;
            for (Long roleId : roleIds) {
                LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getRoleId, roleId)
                    .eq(SysUser::getStatus, 1);
                if (deptId != null) wrapper.eq(SysUser::getDeptId, deptId);
                List<SysUser> users = userMapper.selectList(wrapper);
                for (SysUser u : users) {
                    if (!result.contains(u.getId())) result.add(u.getId());
                }
            }
        } else if ("user".equals(approverType)) {
            result.addAll(values);
        } else if ("mixed".equals(approverType)) {
            List<Long> roleIds = new ArrayList<>();
            List<Long> userIds = new ArrayList<>();
            for (Long v : values) {
                if (v < 1000) roleIds.add(v);
                else userIds.add(v);
            }
            for (Long roleId : roleIds) {
                LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getRoleId, roleId)
                    .eq(SysUser::getStatus, 1);
                if (deptId != null) wrapper.eq(SysUser::getDeptId, deptId);
                List<SysUser> users = userMapper.selectList(wrapper);
                for (SysUser u : users) {
                    if (!result.contains(u.getId())) result.add(u.getId());
                }
            }
            result.addAll(userIds);
        }
        return result;
    }

    /**
     * 解析审批人值列表
     */
    private List<Long> parseApproverValues(Object approverValueObj) {
        List<Long> result = new ArrayList<>();
        if (approverValueObj == null) return result;
        if (approverValueObj instanceof JSONArray) {
            JSONArray array = (JSONArray) approverValueObj;
            for (int i = 0; i < array.size(); i++) {
                Object val = array.get(i);
                if (val instanceof Number) result.add(((Number) val).longValue());
                else if (val instanceof String && StrUtil.isNotBlank((String) val))
                    result.add(Long.parseLong((String) val));
            }
        } else if (approverValueObj instanceof Number) {
            result.add(((Number) approverValueObj).longValue());
        }
        return result;
    }

    /**
     * 解析当前审批人ID列表
     */
    private List<Long> parseCurrentApproverIds(String currentApproverIds) {
        if (StrUtil.isBlank(currentApproverIds)) {
            return new ArrayList<>();
        }
        String[] ids = currentApproverIds.split(",");
        List<Long> result = new ArrayList<>();
        for (String id : ids) {
            if (StrUtil.isNotBlank(id)) {
                result.add(Long.parseLong(id.trim()));
            }
        }
        return result;
    }

    /**
     * 获取节点名称
     */
    private String getNodeName(JSONArray nodes, Integer nodeOrder) {
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            if (nodeOrder.equals(node.getInt("nodeOrder"))) {
                return node.getStr("nodeName");
            }
        }
        return "未知节点";
    }

    /**
     * 获取节点配置
     */
    private JSONObject getNodeConfig(JSONArray nodes, Integer nodeOrder) {
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            if (nodeOrder.equals(node.getInt("nodeOrder"))) {
                return node;
            }
        }
        return null;
    }
}