package com.baoying.vehicleapplication.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baoying.vehicleapplication.common.BusinessException;
import com.baoying.vehicleapplication.dto.request.TemplateRequest;
import com.baoying.vehicleapplication.dto.response.TemplateListItem;
import com.baoying.vehicleapplication.dto.response.TemplateResponse;
import com.baoying.vehicleapplication.entity.ProcessApprover;
import com.baoying.vehicleapplication.entity.ProcessTemplate;
import com.baoying.vehicleapplication.entity.SysUser;
import com.baoying.vehicleapplication.mapper.ProcessApproverMapper;
import com.baoying.vehicleapplication.mapper.ProcessTemplateMapper;
import com.baoying.vehicleapplication.mapper.UserMapper;
import com.baoying.vehicleapplication.service.ProcessTemplateService;
import com.baoying.vehicleapplication.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProcessTemplateServiceImpl extends ServiceImpl<ProcessTemplateMapper, ProcessTemplate> 
        implements ProcessTemplateService {

    @Autowired
    private UserService userService;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ProcessApproverMapper approverMapper;

    @Override
    @Transactional
    public void addTemplate(TemplateRequest request, Long createBy) {
        // 1. 校验模板名称是否重复
        if (isTemplateNameExist(request.getName(), null)) {
            throw new BusinessException("模板名称已存在");
        }

        // 2. 校验节点配置
        validateNodeConfig(request.getNodeConfig());

        // 3. 创建模板
        ProcessTemplate template = new ProcessTemplate();
        BeanUtils.copyProperties(request, template);
        template.setNodeConfig(JSONUtil.toJsonStr(request.getNodeConfig()));
        template.setCreateBy(createBy);
        if (template.getStatus() == null) {
            template.setStatus(1);
        }
        this.save(template);

        // 4. 预置审批人（根据模板中的角色/用户配置）
        preAssignApprovers(template.getTemplateId(), request.getNodeConfig());
    }
    @Override
    @Transactional
    public void updateTemplate(TemplateRequest request) {
        // 1. 检查模板是否存在
        ProcessTemplate existTemplate = this.getById(request.getTemplateId());
        if (existTemplate == null) {
            throw new BusinessException("模板不存在");
        }

        // 2. 校验模板名称是否重复（排除自身）
        if (isTemplateNameExist(request.getName(), request.getTemplateId())) {
            throw new BusinessException("模板名称已存在");
        }

        // 3. 校验节点配置
        validateNodeConfig(request.getNodeConfig());

        // 4. 更新模板
        ProcessTemplate template = new ProcessTemplate();
        BeanUtils.copyProperties(request, template);
        template.setNodeConfig(JSONUtil.toJsonStr(request.getNodeConfig()));
        this.updateById(template);

        // 5. 重新预置审批人
        LambdaQueryWrapper<ProcessApprover> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessApprover::getTemplateId, request.getTemplateId());
        approverMapper.delete(wrapper);
        preAssignApprovers(request.getTemplateId(), request.getNodeConfig());
    }
    private void preAssignApprovers(Integer templateId, List<TemplateRequest.NodeConfig> nodes) {
        for (TemplateRequest.NodeConfig node : nodes) {
            List<Long> userIds = new ArrayList<>();

            if ("role".equals(node.getApproverType())) {
                // 角色类型：查询该角色下所有启用的用户
                for (Long roleId : node.getApproverValue()) {
                    List<SysUser> users = userMapper.selectList(
                            new LambdaQueryWrapper<SysUser>()
                                    .eq(SysUser::getRoleId, roleId)
                                    .eq(SysUser::getStatus, 1)
                    );
                    for (SysUser user : users) {
                        userIds.add(user.getId());
                    }
                }
            } else if ("user".equals(node.getApproverType())) {
                // 用户类型：直接使用指定的用户ID
                userIds.addAll(node.getApproverValue().stream()
                        .map(Long::valueOf)
                        .collect(Collectors.toList()));
            }

            // 插入预置审批人
            for (Long userId : userIds) {
                ProcessApprover approver = new ProcessApprover();
                approver.setTemplateId(templateId);
                approver.setNodeOrder(node.getNodeOrder());
                approver.setUserId(userId);
                approverMapper.insert(approver);
            }
        }
    }

    @Override
    @Transactional
    public void deleteTemplate(Integer templateId) {
        ProcessTemplate template = this.getById(templateId);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        
        // 检查是否有未完成的用车申请关联该模板
        Integer unfinishedCount = baseMapper.countUnfinishedApplyByTemplateId(templateId);
        if (unfinishedCount > 0) {
            throw new BusinessException("该模板存在 " + unfinishedCount + " 个未完成的用车申请，无法删除");
        }
        
        this.removeById(templateId);
    }

    @Override
    @Transactional
    public void updateStatus(Integer templateId, Integer status) {
        ProcessTemplate template = this.getById(templateId);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        
        template.setStatus(status);
        this.updateById(template);
    }

    @Override
    public List<TemplateListItem> listTemplates(String name, Integer status) {
        LambdaQueryWrapper<ProcessTemplate> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(name)) {
            wrapper.like(ProcessTemplate::getName, name);
        }
        if (status != null) {
            wrapper.eq(ProcessTemplate::getStatus, status);
        }
        wrapper.orderByDesc(ProcessTemplate::getCreateTime);
        
        List<ProcessTemplate> templates = this.list(wrapper);
        
        return templates.stream().map(template -> {
            TemplateListItem item = new TemplateListItem();
            BeanUtils.copyProperties(template, item);
            
            // 设置类型名称
            item.setTypeName(getTypeName(template.getType()));
            
            // 获取创建人姓名
            if (template.getCreateBy() != null) {
                SysUser createUser = userService.getById(template.getCreateBy());
                if (createUser != null) {
                    item.setCreateByName(createUser.getRealname());
                }
            }
            
            // 计算节点数量
            List<?> nodes = JSONUtil.parseArray(template.getNodeConfig()).toList(Object.class);
            item.setNodeCount(nodes != null ? nodes.size() : 0);
            
            return item;
        }).collect(Collectors.toList());
    }

    @Override
    public TemplateResponse getTemplateDetail(Integer templateId) {
        ProcessTemplate template = this.getById(templateId);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }
        
        TemplateResponse response = new TemplateResponse();
        BeanUtils.copyProperties(template, response);
        
        // 设置类型名称
        response.setTypeName(getTypeName(template.getType()));
        
        // 解析节点配置
        List<TemplateResponse.NodeConfig> nodeConfigs = JSONUtil.toList(
            JSONUtil.parseArray(template.getNodeConfig()), 
            TemplateResponse.NodeConfig.class
        );
        response.setNodeConfig(nodeConfigs);
        
        // 获取创建人姓名
        if (template.getCreateBy() != null) {
            SysUser createUser = userService.getById(template.getCreateBy());
            if (createUser != null) {
                response.setCreateByName(createUser.getRealname());
            }
        }
        
        return response;
    }

    @Override
    public boolean isTemplateNameExist(String name, Integer excludeId) {
        LambdaQueryWrapper<ProcessTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessTemplate::getName, name);
        if (excludeId != null) {
            wrapper.ne(ProcessTemplate::getTemplateId, excludeId);
        }
        return this.count(wrapper) > 0;
    }
    
    /**
     * 校验节点配置
     */
    private void validateNodeConfig(List<TemplateRequest.NodeConfig> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            throw new BusinessException("至少需要一个审批节点");
        }
        
        for (TemplateRequest.NodeConfig node : nodes) {
            // 校验节点顺序
            if (node.getNodeOrder() == null || node.getNodeOrder() < 1) {
                throw new BusinessException("节点顺序必须为正整数");
            }
            
            // 校验审批人类型
            if (!"user".equals(node.getApproverType()) && !"role".equals(node.getApproverType())) {
                throw new BusinessException("审批人类型必须是 user 或 role");
            }
            
            // 校验审批人值
            if (node.getApproverValue() == null || node.getApproverValue().isEmpty()) {
                throw new BusinessException("节点【" + node.getNodeName() + "】至少需要一个审批人");
            }
            
            // 校验审批方式
            if (!"single".equals(node.getApproveType()) && !"all".equals(node.getApproveType())) {
                throw new BusinessException("审批方式必须是 single 或 all");
            }
        }
        
        // 校验节点顺序是否连续且从1开始
        List<Integer> orders = nodes.stream()
                .map(TemplateRequest.NodeConfig::getNodeOrder)
                .sorted()
                .collect(Collectors.toList());
        
        if (orders.get(0) != 1) {
            throw new BusinessException("节点顺序必须从1开始");
        }
        
        for (int i = 0; i < orders.size() - 1; i++) {
            if (orders.get(i + 1) != orders.get(i) + 1) {
                throw new BusinessException("节点顺序必须连续，缺少第 " + (orders.get(i) + 1) + " 个节点");
            }
        }
    }
    
    /**
     * 获取类型名称
     */
    private String getTypeName(Integer type) {
        if (type == null) return "未知";
        switch (type) {
            case 1: return "内部用车";
            case 2: return "跨部门用车";
            case 3: return "长途用车";
            default: return "未知";
        }
    }
}