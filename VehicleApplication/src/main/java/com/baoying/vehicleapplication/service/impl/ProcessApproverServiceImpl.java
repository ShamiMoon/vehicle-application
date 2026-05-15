package com.baoying.vehicleapplication.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baoying.vehicleapplication.common.BusinessException;
import com.baoying.vehicleapplication.dto.request.ApproverAssignBatchRequest;
import com.baoying.vehicleapplication.dto.request.ApproverAssignRequest;
import com.baoying.vehicleapplication.dto.request.ApproverUpdateRequest;
import com.baoying.vehicleapplication.dto.response.ApproverNodeResponse;
import com.baoying.vehicleapplication.entity.ProcessApprover;
import com.baoying.vehicleapplication.entity.ProcessTemplate;
import com.baoying.vehicleapplication.entity.SysRole;
import com.baoying.vehicleapplication.entity.SysUser;
import com.baoying.vehicleapplication.mapper.ProcessApproverMapper;
import com.baoying.vehicleapplication.mapper.ProcessTemplateMapper;
import com.baoying.vehicleapplication.mapper.RoleMapper;
import com.baoying.vehicleapplication.mapper.UserMapper;
import com.baoying.vehicleapplication.service.ProcessApproverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.baoying.vehicleapplication.utils.CollectionUtils.distinct;

@Service
public class ProcessApproverServiceImpl extends ServiceImpl<ProcessApproverMapper, ProcessApprover>
        implements ProcessApproverService {

    private static final long ROLE_ID_THRESHOLD = 1000;
    @Autowired
    private ProcessTemplateMapper templateMapper;

    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional
    public void preAssignApprovers(Integer templateId, List<?> nodeConfigList) {
        // 先删除该模板已有的所有预置审批人
        baseMapper.deleteByTemplateId(templateId);

        if (nodeConfigList == null || nodeConfigList.isEmpty()) {
            return;
        }

        JSONArray nodes = JSONUtil.parseArray(nodeConfigList);

        for (int i = 0; i < nodes.size(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            Integer nodeOrder = node.getInt("nodeOrder");
            String approverType = node.getStr("approverType");
            Object approverValueObj = node.get("approverValue");

            if (approverValueObj == null) {
                continue;
            }

            List<Long> userIds = new ArrayList<>();

            if ("role".equals(approverType)) {
                List<Long> roleIds = parseApproverValues(approverValueObj);
                for (Long roleId : roleIds) {
                    List<SysUser> users = userMapper.selectList(
                            new LambdaQueryWrapper<SysUser>()
                                    .eq(SysUser::getRoleId, roleId)
                                    .eq(SysUser::getStatus, 1)
                    );
                    for (SysUser user : users) {
                        if (!userIds.contains(user.getId())) {
                            userIds.add(user.getId());
                        }
                    }
                }
            } else if ("user".equals(approverType)) {
                userIds = parseApproverValues(approverValueObj);
            }

            for (Long userId : userIds) {
                ProcessApprover approver = new ProcessApprover();
                approver.setTemplateId(templateId);
                approver.setNodeOrder(nodeOrder);
                approver.setUserId(userId);
                this.save(approver);
            }
        }
    }

    private List<Long> extractRoleIds(Object approverValueObj) {
        List<Long> result = new ArrayList<>();
        List<Object> list = parseApproverValuesAsObjects(approverValueObj);
        for (Object obj : list) {
            if (obj instanceof Long) {
                Long id = (Long) obj;
                if (id < ROLE_ID_THRESHOLD) {
                    result.add(id);
                }
            }
        }
        return result;
    }

    private List<Long> extractUserIds(Object approverValueObj) {
        List<Long> result = new ArrayList<>();
        List<Object> list = parseApproverValuesAsObjects(approverValueObj);
        for (Object obj : list) {
            if (obj instanceof Long) {
                Long id = (Long) obj;
                if (id >= ROLE_ID_THRESHOLD) {
                    result.add(id);
                }
            }
        }
        return result;
    }

    @Override
    @Transactional
    public void assignApprovers(ApproverAssignRequest request) {
        // 1. 校验模板...
        ProcessTemplate template = templateMapper.selectById(request.getTemplateId());
        if (template == null) {
            throw new BusinessException("流程模板不存在");
        }
        if (template.getStatus() != 1) {
            throw new BusinessException("流程模板已禁用，无法分配审批人");
        }

        // 2. 验证用户存在且启用
        for (Long userId : request.getUserIds()) {
            SysUser user = userMapper.selectById(userId);
            if (user == null) {
                throw new BusinessException("用户ID " + userId + " 不存在");
            }
            if (user.getStatus() != 1) {
                throw new BusinessException("用户 " + user.getRealname() + " 已被禁用");
            }
        }

        // 3. 解析模板配置
        JSONArray nodes = JSONUtil.parseArray(template.getNodeConfig());
        JSONObject targetNode = null;
        int nodeIndex = -1;
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            if (request.getNodeOrder().equals(node.getInt("nodeOrder"))) {
                targetNode = node;
                nodeIndex = i;
                break;
            }
        }
        if (targetNode == null) {
            throw new BusinessException("节点顺序 " + request.getNodeOrder() + " 不存在于模板配置中");
        }

        // 4. 获取当前配置
        String currentType = targetNode.getStr("approverType");
        Object currentValue = targetNode.get("approverValue");

        List<Long> existingRoleIds = extractRoleIds(currentValue);
        List<Long> existingUserIds = extractUserIds(currentValue);

        // 5. 追加新用户
        for (Long userId : request.getUserIds()) {
            if (!existingUserIds.contains(userId)) {
                existingUserIds.add(userId);
            }
        }

        // 6. 确定新的类型和值
        List<Object> newApproverValues = new ArrayList<>();
        newApproverValues.addAll(existingRoleIds);
        newApproverValues.addAll(existingUserIds);

        String newType;
        if (existingRoleIds.isEmpty() && !existingUserIds.isEmpty()) {
            newType = "user";
        } else if (!existingRoleIds.isEmpty() && existingUserIds.isEmpty()) {
            newType = "role";
        } else {
            newType = "mixed";
        }

        // 7. 更新模板配置
        targetNode.put("approverType", newType);
        targetNode.put("approverValue", newApproverValues);
        nodes.set(nodeIndex, targetNode);
        template.setNodeConfig(JSONUtil.toJsonStr(nodes));
        templateMapper.updateById(template);

        // 8. 同步更新 process_approver 表
        for (Long userId : request.getUserIds()) {
            ProcessApprover approver = new ProcessApprover();
            approver.setTemplateId(request.getTemplateId());
            approver.setNodeOrder(request.getNodeOrder());
            approver.setUserId(userId);

            LambdaQueryWrapper<ProcessApprover> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ProcessApprover::getTemplateId, request.getTemplateId())
                    .eq(ProcessApprover::getNodeOrder, request.getNodeOrder())
                    .eq(ProcessApprover::getUserId, userId);
            if (this.count(wrapper) == 0) {
                this.save(approver);
            }
        }
    }


    @Override
    @Transactional
    public void updateApprovers(ApproverUpdateRequest request) {
        // 1. 校验模板是否存在
        ProcessTemplate template = templateMapper.selectById(request.getTemplateId());
        if (template == null) {
            throw new BusinessException("流程模板不存在");
        }
        if (template.getStatus() != 1) {
            throw new BusinessException("流程模板已禁用，无法修改审批人");
        }

        // 2. 校验审批人类型
        if (!Arrays.asList("role", "user", "mixed").contains(request.getApproverType())) {
            throw new BusinessException("审批人类型必须是 role、user 或 mixed");
        }

        // 3. 校验审批人值不为空
        if (request.getApproverValue() == null || request.getApproverValue().isEmpty()) {
            throw new BusinessException("审批人值不能为空");
        }

        // 4. 根据类型校验并获取有效用户ID（用于 process_approver 表）
        List<Long> effectiveUserIds = new ArrayList<>();

        if ("role".equals(request.getApproverType())) {
            // 角色类型：只允许角色ID，不能有用户ID
            for (Long id : request.getApproverValue()) {
                if (id >= ROLE_ID_THRESHOLD) {
                    throw new BusinessException("角色类型只能包含角色ID（小于" + ROLE_ID_THRESHOLD + "）");
                }
            }
            // 角色类型：查询该角色下所有启用的用户，用于 process_approver 表
            for (Long roleId : request.getApproverValue()) {
                List<SysUser> users = userMapper.selectList(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(SysUser::getRoleId, roleId)
                                .eq(SysUser::getStatus, 1)
                );
                for (SysUser user : users) {
                    if (!effectiveUserIds.contains(user.getId())) {
                        effectiveUserIds.add(user.getId());
                    }
                }
            }
        } else if ("user".equals(request.getApproverType())) {
            // 用户类型：只允许用户ID，不能有角色ID
            for (Long id : request.getApproverValue()) {
                if (id < ROLE_ID_THRESHOLD) {
                    throw new BusinessException("用户类型只能包含用户ID（大于等于" + ROLE_ID_THRESHOLD + "）");
                }
            }
            effectiveUserIds = new ArrayList<>(request.getApproverValue());

            // 验证用户存在且启用
            for (Long userId : effectiveUserIds) {
                SysUser user = userMapper.selectById(userId);
                if (user == null) {
                    throw new BusinessException("用户ID " + userId + " 不存在");
                }
                if (user.getStatus() != 1) {
                    throw new BusinessException("用户 " + user.getRealname() + " 已被禁用");
                }
            }
        } else if ("mixed".equals(request.getApproverType())) {
            // 混合类型：可以同时包含角色ID和用户ID
            List<Long> roleIds = new ArrayList<>();
            List<Long> userIds = new ArrayList<>();

            for (Long id : request.getApproverValue()) {
                if (id < ROLE_ID_THRESHOLD) {
                    roleIds.add(id);
                } else {
                    userIds.add(id);
                }
            }

            // 验证用户存在且启用
            for (Long userId : userIds) {
                SysUser user = userMapper.selectById(userId);
                if (user == null) {
                    throw new BusinessException("用户ID " + userId + " 不存在");
                }
                if (user.getStatus() != 1) {
                    throw new BusinessException("用户 " + user.getRealname() + " 已被禁用");
                }
            }

            // 混合类型：角色动态查询 + 固定用户
            for (Long roleId : roleIds) {
                List<SysUser> users = userMapper.selectList(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(SysUser::getRoleId, roleId)
                                .eq(SysUser::getStatus, 1)
                );
                for (SysUser user : users) {
                    if (!effectiveUserIds.contains(user.getId())) {
                        effectiveUserIds.add(user.getId());
                    }
                }
            }
            effectiveUserIds.addAll(userIds);
        }

        if (effectiveUserIds.isEmpty()) {
            throw new BusinessException("未找到有效的审批人");
        }

        // 5. 解析模板配置，找到目标节点
        JSONArray nodes = JSONUtil.parseArray(template.getNodeConfig());
        JSONObject targetNode = null;
        int nodeIndex = -1;
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            if (request.getNodeOrder().equals(node.getInt("nodeOrder"))) {
                targetNode = node;
                nodeIndex = i;
                break;
            }
        }
        if (targetNode == null) {
            throw new BusinessException("节点顺序 " + request.getNodeOrder() + " 不存在于模板配置中");
        }

        // 6. 更新模板配置
        targetNode.put("approverType", request.getApproverType());
        targetNode.put("approverValue", request.getApproverValue());
        nodes.set(nodeIndex, targetNode);
        template.setNodeConfig(JSONUtil.toJsonStr(nodes));
        templateMapper.updateById(template);

        // 7. 同步更新 process_approver 表
        LambdaQueryWrapper<ProcessApprover> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(ProcessApprover::getTemplateId, request.getTemplateId())
                .eq(ProcessApprover::getNodeOrder, request.getNodeOrder());
        this.remove(deleteWrapper);

        for (Long userId : effectiveUserIds) {
            ProcessApprover approver = new ProcessApprover();
            approver.setTemplateId(request.getTemplateId());
            approver.setNodeOrder(request.getNodeOrder());
            approver.setUserId(userId);
            this.save(approver);
        }
    }

    @Override
    @Transactional
    public void batchAssignApprovers(ApproverAssignBatchRequest request) {
        for (ApproverAssignRequest item : request.getItems()) {
            assignApprovers(item);
        }
    }

    @Override
    @Transactional
    public void deleteApprover(Integer templateId, Integer nodeOrder, Long userId) {
        ProcessTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException("流程模板不存在");
        }

        JSONArray nodes = JSONUtil.parseArray(template.getNodeConfig());
        JSONObject targetNode = null;
        int nodeIndex = -1;
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            if (nodeOrder.equals(node.getInt("nodeOrder"))) {
                targetNode = node;
                nodeIndex = i;
                break;
            }
        }
        if (targetNode == null) {
            throw new BusinessException("节点顺序 " + nodeOrder + " 不存在于模板配置中");
        }

        // 从配置中移除该用户ID
        Object approverValueObj = targetNode.get("approverValue");
        if (approverValueObj != null) {
            List<Object> newApproverValues = new ArrayList<>();
            List<Object> existing = parseApproverValuesAsObjects(approverValueObj);
            for (Object val : existing) {
                if (val instanceof Long) {
                    Long id = (Long) val;
                    if (!id.equals(userId)) {
                        newApproverValues.add(val);
                    }
                } else {
                    newApproverValues.add(val);
                }
            }
            targetNode.put("approverValue", newApproverValues);
            nodes.set(nodeIndex, targetNode);
            template.setNodeConfig(JSONUtil.toJsonStr(nodes));
            templateMapper.updateById(template);
        }

        // 从 process_approver 表中删除
        LambdaQueryWrapper<ProcessApprover> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessApprover::getTemplateId, templateId)
                .eq(ProcessApprover::getNodeOrder, nodeOrder)
                .eq(ProcessApprover::getUserId, userId);
        this.remove(wrapper);
    }


    @Override
    @Transactional
    public void deleteAllApproversOfNode(Integer templateId, Integer nodeOrder) {
        ProcessTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException("流程模板不存在");
        }

        // 解析模板配置
        JSONArray nodes = JSONUtil.parseArray(template.getNodeConfig());
        JSONObject targetNode = null;
        int nodeIndex = -1;
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            if (nodeOrder.equals(node.getInt("nodeOrder"))) {
                targetNode = node;
                nodeIndex = i;
                break;
            }
        }
        if (targetNode == null) {
            throw new BusinessException("节点顺序 " + nodeOrder + " 不存在于模板配置中");
        }

        // 清空该节点的审批人配置（保留类型但清空值）
        targetNode.put("approverValue", new ArrayList<>());
        nodes.set(nodeIndex, targetNode);
        template.setNodeConfig(JSONUtil.toJsonStr(nodes));
        templateMapper.updateById(template);

        // 删除 process_approver 表中的相关记录
        LambdaQueryWrapper<ProcessApprover> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessApprover::getTemplateId, templateId)
                .eq(ProcessApprover::getNodeOrder, nodeOrder);
        this.remove(wrapper);
    }

    @Override
    @Transactional
    public void deleteAllApproversOfTemplate(Integer templateId) {
        ProcessTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException("流程模板不存在");
        }

        // 解析模板配置
        JSONArray nodes = JSONUtil.parseArray(template.getNodeConfig());
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            // 清空审批人值，但保留类型
            node.put("approverValue", new ArrayList<>());
        }
        template.setNodeConfig(JSONUtil.toJsonStr(nodes));
        templateMapper.updateById(template);

        // 删除 process_approver 表中的所有相关记录
        LambdaQueryWrapper<ProcessApprover> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessApprover::getTemplateId, templateId);
        this.remove(wrapper);
    }


//    获取节点审批人
    public List<Long> getNodeApproverUserIds(Integer templateId, Integer nodeOrder, Integer deptId) {
        ProcessTemplate template = templateMapper.selectById(templateId);
        JSONArray nodes = JSONUtil.parseArray(template.getNodeConfig());

        JSONObject targetNode = null;
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            if (nodeOrder.equals(node.getInt("nodeOrder"))) {
                targetNode = node;
                break;
            }
        }
        if (targetNode == null) {
            return Collections.emptyList();
        }

        String approverType = targetNode.getStr("approverType");
        Object approverValueObj = targetNode.get("approverValue");

        List<Long> result = new ArrayList<>();

        if ("role".equals(approverType)) {
            // 只有角色：动态查询
            List<Long> roleIds = extractRoleIds(approverValueObj);
            for (Long roleId : roleIds) {
                List<SysUser> users = userMapper.selectList(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(SysUser::getRoleId, roleId)
                                .eq(SysUser::getDeptId, deptId)
                                .eq(SysUser::getStatus, 1)
                );
                for (SysUser user : users) {
                    if (!result.contains(user.getId())) {
                        result.add(user.getId());
                    }
                }
            }
        } else if ("user".equals(approverType)) {
            // 只有用户：直接返回
            result.addAll(extractUserIds(approverValueObj));
        } else if ("mixed".equals(approverType)) {
            // 组合：角色动态查询 + 固定用户
            List<Long> roleIds = extractRoleIds(approverValueObj);
            for (Long roleId : roleIds) {
                List<SysUser> users = userMapper.selectList(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(SysUser::getRoleId, roleId)
                                .eq(SysUser::getDeptId, deptId)
                                .eq(SysUser::getStatus, 1)
                );
                for (SysUser user : users) {
                    if (!result.contains(user.getId())) {
                        result.add(user.getId());
                    }
                }
            }
            result.addAll(extractUserIds(approverValueObj));
        }

        return result;
    }

    @Override
    public List<Long> getApproverUserIds(Integer templateId, Integer nodeOrder) {
        ProcessTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException("流程模板不存在");
        }

        JSONArray nodes = JSONUtil.parseArray(template.getNodeConfig());
        JSONObject targetNode = null;
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            if (nodeOrder.equals(node.getInt("nodeOrder"))) {
                targetNode = node;
                break;
            }
        }
        if (targetNode == null) {
            throw new BusinessException("节点顺序 " + nodeOrder + " 不存在于模板配置中");
        }

        String approverType = targetNode.getStr("approverType");
        Object approverValueObj = targetNode.get("approverValue");

        List<Long> result = new ArrayList<>();

        if ("role".equals(approverType)) {
            List<Long> roleIds = extractRoleIds(approverValueObj);
            for (Long roleId : roleIds) {
                List<SysUser> users = userMapper.selectList(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(SysUser::getRoleId, roleId)
                                .eq(SysUser::getStatus, 1)
                );
                for (SysUser user : users) {
                    if (!result.contains(user.getId())) {
                        result.add(user.getId());
                    }
                }
            }
        } else if ("user".equals(approverType)) {
            // 用户类型：直接返回
            result.addAll(extractUserIds(approverValueObj));
        } else if ("mixed".equals(approverType)) {
            // 混合类型：角色查询 + 固定用户
            List<Long> roleIds = extractRoleIds(approverValueObj);
            for (Long roleId : roleIds) {
                List<SysUser> users = userMapper.selectList(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(SysUser::getRoleId, roleId)
                                .eq(SysUser::getStatus, 1)
                );
                for (SysUser user : users) {
                    if (!result.contains(user.getId())) {
                        result.add(user.getId());
                    }
                }
            }
            result.addAll(extractUserIds(approverValueObj));
        }

        return distinct(result);
    }

    @Override
    public List<ApproverNodeResponse> getApproversByTemplate(Integer templateId) {
        ProcessTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException("流程模板不存在");
        }

        JSONArray nodes = JSONUtil.parseArray(template.getNodeConfig());
        List<ApproverNodeResponse> result = new ArrayList<>();

        for (int i = 0; i < nodes.size(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            Integer nodeOrder = node.getInt("nodeOrder");
            String nodeName = node.getStr("nodeName");
            String approverType = node.getStr("approverType");
            Object approverValueObj = node.get("approverValue");

            ApproverNodeResponse response = new ApproverNodeResponse();
            response.setNodeOrder(nodeOrder);
            response.setNodeName(nodeName);
            response.setApproverType(approverType);

            List<Long> userIds = new ArrayList<>();

            if ("role".equals(approverType)) {
                // 角色类型：查询该角色下所有启用的用户（不限制部门）
                List<Long> roleIds = extractRoleIds(approverValueObj);
                for (Long roleId : roleIds) {
                    List<SysUser> users = userMapper.selectList(
                            new LambdaQueryWrapper<SysUser>()
                                    .eq(SysUser::getRoleId, roleId)
                                    .eq(SysUser::getStatus, 1)
                    );
                    for (SysUser user : users) {
                        if (!userIds.contains(user.getId())) {
                            userIds.add(user.getId());
                        }
                    }
                }
            } else if ("user".equals(approverType)) {
                // 用户类型：直接返回
                userIds.addAll(extractUserIds(approverValueObj));
            } else if ("mixed".equals(approverType)) {
                // 混合类型：角色查询 + 固定用户
                List<Long> roleIds = extractRoleIds(approverValueObj);
                for (Long roleId : roleIds) {
                    List<SysUser> users = userMapper.selectList(
                            new LambdaQueryWrapper<SysUser>()
                                    .eq(SysUser::getRoleId, roleId)
                                    .eq(SysUser::getStatus, 1)
                    );
                    for (SysUser user : users) {
                        if (!userIds.contains(user.getId())) {
                            userIds.add(user.getId());
                        }
                    }
                }
                userIds.addAll(extractUserIds(approverValueObj));
            }

            response.setUserIds(distinct(userIds));
            result.add(response);
        }

        // 按节点顺序排序
        result.sort(Comparator.comparing(ApproverNodeResponse::getNodeOrder));
        return result;
    }

    @Override
    public boolean isApprover(Integer templateId, Integer nodeOrder, Long userId) {
        ProcessTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            return false;
        }

        JSONArray nodes = JSONUtil.parseArray(template.getNodeConfig());
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            if (nodeOrder.equals(node.getInt("nodeOrder"))) {
                Object approverValueObj = node.get("approverValue");
                if (approverValueObj != null) {
                    List<Long> userIds = parseApproverValues(approverValueObj);
                    return userIds.contains(userId);
                }
                break;
            }
        }
        return false;
    }

    /**
     * 解析 approverValue 字段（可能是单个值或数组）
     */
    private List<Long> parseApproverValues(Object approverValueObj) {
        List<Long> result = new ArrayList<>();
        if (approverValueObj == null) {
            return result;
        }
        if (approverValueObj instanceof JSONArray) {
            JSONArray array = (JSONArray) approverValueObj;
            for (int i = 0; i < array.size(); i++) {
                Object val = array.get(i);
                if (val instanceof Number) {
                    result.add(((Number) val).longValue());
                } else if (val instanceof String && StrUtil.isNotBlank((String) val)) {
                    result.add(Long.parseLong((String) val));
                }
            }
        } else if (approverValueObj instanceof Number) {
            result.add(((Number) approverValueObj).longValue());
        } else if (approverValueObj instanceof String && StrUtil.isNotBlank((String) approverValueObj)) {
            result.add(Long.parseLong((String) approverValueObj));
        }
        return result;
    }
    private List<Object> parseApproverValuesAsObjects(Object approverValueObj) {
        List<Object> result = new ArrayList<>();
        if (approverValueObj == null) {
            return result;
        }
        if (approverValueObj instanceof JSONArray) {
            JSONArray array = (JSONArray) approverValueObj;
            for (int i = 0; i < array.size(); i++) {
                Object val = array.get(i);
                if (val instanceof Number) {
                    result.add(((Number) val).longValue());
                } else if (val instanceof String && StrUtil.isNotBlank((String) val)) {
                    result.add(Long.parseLong((String) val));
                }
            }
        } else if (approverValueObj instanceof Number) {
            result.add(((Number) approverValueObj).longValue());
        } else if (approverValueObj instanceof String && StrUtil.isNotBlank((String) approverValueObj)) {
            result.add(Long.parseLong((String) approverValueObj));
        }
        return result;
    }
    private boolean containsUserId(List<Object> list, Long userId) {
        for (Object obj : list) {
            if (obj instanceof Long) {
                Long id = (Long) obj;
                if (id.equals(userId)) {
                    return true;
                }
            }
        }
        return false;
    }
}