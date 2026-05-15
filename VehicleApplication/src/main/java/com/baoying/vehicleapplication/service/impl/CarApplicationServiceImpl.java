package com.baoying.vehicleapplication.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baoying.vehicleapplication.common.ApplyStatusEnum;
import com.baoying.vehicleapplication.common.VehicleTypeEnum;
import com.baoying.vehicleapplication.common.BusinessException;
import com.baoying.vehicleapplication.dto.request.CarApplyQueryRequest;
import com.baoying.vehicleapplication.dto.request.CarApplySaveRequest;
import com.baoying.vehicleapplication.dto.request.CarApplySubmitRequest;
import com.baoying.vehicleapplication.dto.request.CarApplyUpdateRequest;
import com.baoying.vehicleapplication.dto.response.CarApplyDetailResponse;
import com.baoying.vehicleapplication.dto.response.CarApplyExportDTO;
import com.baoying.vehicleapplication.dto.response.CarApplyListItem;
import com.baoying.vehicleapplication.entity.*;
import com.baoying.vehicleapplication.mapper.*;
import com.baoying.vehicleapplication.service.CarApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.baoying.vehicleapplication.utils.CollectionUtils.distinct;

@Service
@RequiredArgsConstructor
public class CarApplicationServiceImpl extends ServiceImpl<CarApplicationMapper, CarApplication>
        implements CarApplicationService {

    private final CarApplicationMapper carApplicationMapper;
    private final ProcessTemplateMapper templateMapper;
    private final ProcessHistoryMapper historyMapper;
    private final UserMapper userMapper;
    private final DeptMapper deptMapper;
    private final RoleMapper roleMapper;
    private final com.baoying.vehicleapplication.service.ApprovalNotificationService notificationService;
    private final DeptRoleMapper deptRoleMapper;
    private final com.baoying.vehicleapplication.service.DeptService deptService;

    private static final long ROLE_ID_THRESHOLD = 1000;

    @Override
    @Transactional
    public Long saveApplication(CarApplySaveRequest request, Long userId, Integer deptId){
        SysUser currentUser = userMapper.selectById(userId);
        if (currentUser == null) {
            throw new BusinessException("用户不存在");
        }
        ProcessTemplate template = templateMapper.selectById(request.getTemplateId());
        if (template == null) {
            throw new BusinessException("流程模板不存在");
        }
        Integer targetDeptId = calculateTargetDeptId(template.getType(), deptId, request.getTargetDeptId());


        CarApplication application = new CarApplication();
        application.setTitle(request.getTitle());
        application.setStartTime(request.getStartTime());
        application.setEndTime(request.getEndTime());
        application.setReason(request.getReason());
        application.setPassengers(request.getPassengers());
        application.setDestination(request.getDestination());
        application.setVehicleType(request.getVehicleType());
        application.setAttachment(request.getAttachment());
        application.setApplyBy(userId);
        application.setDeptId(deptId);
        application.setTemplateId(request.getTemplateId());
        application.setTargetDeptId(targetDeptId);
        application.setStatus(ApplyStatusEnum.DRAFT.getCode());  // 待提交
        application.setIsUrgent(request.getIsUrgent() != null ? request.getIsUrgent() : 0);
        application.setCreateTime(LocalDateTime.now());

        this.save(application);
        return application.getId();
    }

    @Override
    @Transactional
    public void submit(Long applyId, Long userId) {
        CarApplication application = this.getById(applyId);
        if (application == null) {
            throw new BusinessException("申请不存在");
        }
        if (!application.getApplyBy().equals(userId)) {
            throw new BusinessException("只能提交自己的申请");
        }
        if (application.getStatus() != ApplyStatusEnum.DRAFT.getCode()) {
            throw new BusinessException("只有草稿状态的申请可以提交");
        }
        if (StrUtil.isBlank(application.getTitle())) {
            throw new BusinessException("申请标题不能为空");
        }
        if (application.getStartTime() == null) {
            throw new BusinessException("用车开始日期不能为空");
        }
        if (application.getEndTime() == null) {
            throw new BusinessException("用车结束日期不能为空");
        }
        if (application.getStartTime().isAfter(application.getEndTime())) {
            throw new BusinessException("用车开始日期不能晚于结束日期");
        }
        if (application.getStartTime().equals(application.getEndTime())) {
            application.setIsUrgent(1);
        }
        if (application.getIsUrgent() == null || application.getIsUrgent() != 1) {
            if (application.getStartTime().isBefore(LocalDate.now())) {
                throw new BusinessException("用车开始日期必须大于或等于当前日期");
            }
        }
        if (StrUtil.isBlank(application.getReason())) {
            throw new BusinessException("用车事由不能为空");
        }
        if (application.getPassengers() == null || application.getPassengers() <= 0) {
            throw new BusinessException("用车人数不能为空且必须大于0");
        }
        if (StrUtil.isBlank(application.getDestination())) {
            throw new BusinessException("目的地不能为空");
        }
        if (application.getVehicleType() == null) {
            throw new BusinessException("车辆类型不能为空");
        }
        if (application.getTemplateId() == null) {
            throw new BusinessException("流程模板不能为空");
        }

        // 校验流程模板（存在且启用）
        ProcessTemplate template = templateMapper.selectById(application.getTemplateId());
        if (template == null) {
            throw new BusinessException("流程模板不存在");
        }
        if (template.getStatus() != 1) {
            throw new BusinessException("流程模板已禁用，无法提交申请");
        }

        // 校验车辆类型是否适用于该模板
        if (!VehicleTypeEnum.isValidForTemplateType(application.getVehicleType(), template.getType())) {
            List<Integer> availableTypes = VehicleTypeEnum.getAvailableTypesByTemplateType(template.getType());
            String availableNames = availableTypes.stream()
                    .map(VehicleTypeEnum::getNameByCode)
                    .collect(Collectors.joining("、"));
            throw new BusinessException("该流程模板不支持选择的车辆类型，可选类型：" + availableNames);
        }

        // 保存模板节点配置快照（以便后续节点流转时不受模板编辑影响）
        application.setNodeConfigSnapshot(template.getNodeConfig());

        // 根据模板类型更新目标部门
        Integer targetDeptId = calculateTargetDeptId(template.getType(), application.getDeptId(), application.getTargetDeptId());
        application.setTargetDeptId(targetDeptId);


        // 获取第一个节点的审批人
        JSONArray nodes = JSONUtil.parseArray(template.getNodeConfig());
        if (nodes.isEmpty()) {
            throw new BusinessException("流程模板未配置审批节点");
        }

        JSONObject firstNode = nodes.getJSONObject(0);
        Integer firstNodeOrder = firstNode.getInt("nodeOrder");

        List<Long> firstNodeApprovers = getNodeApprovers(
                application.getTemplateId(), firstNodeOrder,
                application.getDeptId(), application.getTargetDeptId()
        );

        if (firstNodeApprovers.isEmpty()) {
            throw new BusinessException("未找到第一个节点的审批人，请联系管理员配置");
        }

        // 更新申请
        application.setCurrentNode(firstNodeOrder);
        application.setCurrentApproverIds(StrUtil.join(",", firstNodeApprovers));
        application.setStatus(ApplyStatusEnum.PENDING.getCode());
        application.setUpdateTime(LocalDateTime.now());
        this.updateById(application);

        // 发送通知给第一个节点的审批人
        for (Long approverId : firstNodeApprovers) {
            notificationService.sendPendingApprovalNotification(
                approverId,
                application.getTitle(),
                application.getId()
            );
        }
    }
    @Override
    @Transactional
    public Long submitApplication(CarApplySubmitRequest request, Long userId, Integer deptId) {
        // 1. 校验日期
        if (request.getStartTime().isAfter(request.getEndTime())) {
            throw new BusinessException("用车开始日期不能晚于结束日期");
        }
        if (request.getStartTime().equals(request.getEndTime())) {
            request.setIsUrgent(1);
        }
        if (request.getIsUrgent() == null || request.getIsUrgent() != 1) {
            if (request.getStartTime().isBefore(LocalDate.now())) {
                throw new BusinessException("用车开始日期必须大于或等于当前日期");
            }
        }

        // 2. 校验流程模板
        ProcessTemplate template = templateMapper.selectById(request.getTemplateId());
        if (template == null) {
            throw new BusinessException("流程模板不存在");
        }
        if (template.getStatus() != 1) {
            throw new BusinessException("流程模板已禁用，无法提交申请");
        }

        // 3. 校验车辆类型
        if (!VehicleTypeEnum.isValidForTemplateType(request.getVehicleType(), template.getType())) {
            List<Integer> availableTypes = VehicleTypeEnum.getAvailableTypesByTemplateType(template.getType());
            String availableNames = availableTypes.stream()
                    .map(VehicleTypeEnum::getNameByCode)
                    .collect(Collectors.joining("、"));
            throw new BusinessException("该流程模板不支持选择的车辆类型，可选类型：" + availableNames);
        }

        // 4. 获取当前用户
        SysUser currentUser = userMapper.selectById(userId);
        if (currentUser == null) {
            throw new BusinessException("用户不存在");
        }

        // 5. 权限校验
        if (template.getType() == 1) {
            if (!currentUser.getDeptId().equals(deptId)) {
                throw new BusinessException("内部用车只能提交本部门申请");
            }
        }
        if (template.getType() == 2 && request.getTargetDeptId() == null) {
            throw new BusinessException("跨部门用车需要指定目标部门");
        }

        // 根据模板类型计算目标部门
        Integer targetDeptId = calculateTargetDeptId(template.getType(), deptId, request.getTargetDeptId());

        // 保存模板节点配置快照
        String nodeConfigSnapshot = template.getNodeConfig();

        // 6. 获取第一个节点的审批人
        JSONArray nodes = JSONUtil.parseArray(template.getNodeConfig());
        if (nodes.isEmpty()) {
            throw new BusinessException("流程模板未配置审批节点");
        }

        JSONObject firstNode = nodes.getJSONObject(0);
        Integer firstNodeOrder = firstNode.getInt("nodeOrder");

        List<Long> firstNodeApprovers = getNodeApprovers(
                request.getTemplateId(), firstNodeOrder, deptId, request.getTargetDeptId()
        );

        if (firstNodeApprovers.isEmpty()) {
            throw new BusinessException("未找到第一个节点的审批人，请联系管理员配置");
        }

        // 7. 创建申请（直接提交）
        CarApplication application = new CarApplication();
        application.setTitle(request.getTitle());
        application.setStartTime(request.getStartTime());
        application.setEndTime(request.getEndTime());
        application.setReason(request.getReason());
        application.setPassengers(request.getPassengers());
        application.setDestination(request.getDestination());
        application.setVehicleType(request.getVehicleType());
        application.setAttachment(request.getAttachment());
        application.setApplyBy(userId);
        application.setDeptId(deptId);
        application.setTemplateId(request.getTemplateId());
        application.setTargetDeptId(targetDeptId);
        application.setCurrentNode(firstNodeOrder);
        application.setCurrentApproverIds(StrUtil.join(",", firstNodeApprovers));
        application.setStatus(ApplyStatusEnum.PENDING.getCode());
        application.setIsUrgent(request.getIsUrgent() != null ? request.getIsUrgent() : 0);
        application.setNodeConfigSnapshot(nodeConfigSnapshot);
        application.setCreateTime(LocalDateTime.now());

        this.save(application);

        // 发送通知给第一个节点的审批人
        for (Long approverId : firstNodeApprovers) {
            notificationService.sendPendingApprovalNotification(
                approverId,
                application.getTitle(),
                application.getId()
            );
        }
        
        return application.getId();
    }


    @Override
    @Transactional
    public void updateApplication(CarApplyUpdateRequest request, Long userId) {
        CarApplication application = this.getById(request.getId());
        if (application == null) {
            throw new BusinessException("申请不存在");
        }

        if (!application.getApplyBy().equals(userId)) {
            throw new BusinessException("只能修改自己的申请");
        }

        if (application.getStatus() != ApplyStatusEnum.DRAFT.getCode() &&
                application.getStatus() != ApplyStatusEnum.REJECTED.getCode()) {
            throw new BusinessException("当前状态不可修改，只能修改待提交或已驳回的申请");
        }

        if (StrUtil.isNotBlank(request.getTitle())) {
            application.setTitle(request.getTitle());
        }
        if (request.getStartTime() != null) {
            application.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            application.setEndTime(request.getEndTime());
        }
        if (StrUtil.isNotBlank(request.getReason())) {
            application.setReason(request.getReason());
        }
        if (request.getPassengers() != null) {
            application.setPassengers(request.getPassengers());
        }
        if (StrUtil.isNotBlank(request.getDestination())) {
            application.setDestination(request.getDestination());
        }
        if (request.getVehicleType() != null) {
            application.setVehicleType(request.getVehicleType());
        }
        if (request.getTemplateId() != null){
            application.setTemplateId(request.getTemplateId());
        }
        if (request.getAttachment() != null) {
            application.setAttachment(request.getAttachment());
        }
        if (request.getIsUrgent() != null) {
            application.setIsUrgent(request.getIsUrgent());
        }
        application.setUpdateTime(LocalDateTime.now());

        this.updateById(application);
    }

    @Override
    @Transactional
    public void cancelApplication(Long applyId, Long userId) {
        CarApplication application = this.getById(applyId);
        if (application == null) {
            throw new BusinessException("申请不存在");
        }

        if (!application.getApplyBy().equals(userId)) {
            throw new BusinessException("只能撤销自己的申请");
        }

        if (application.getStatus() != ApplyStatusEnum.PENDING.getCode() &&
                application.getStatus() != ApplyStatusEnum.PROCESSING.getCode()) {
            throw new BusinessException("当前状态不可撤销");
        }

        application.setStatus(ApplyStatusEnum.CANCELLED.getCode());
        application.setUpdateTime(LocalDateTime.now());
        this.updateById(application);
    }

    @Override
    public Page<CarApplyListItem> getMyApplicationList(CarApplyQueryRequest request, Long userId) {
        LambdaQueryWrapper<CarApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CarApplication::getApplyBy, userId);
        applyQueryConditions(wrapper, request);
        wrapper.orderByDesc(CarApplication::getCreateTime);

        Page<CarApplication> page = this.page(new Page<>(request.getPageNum(), request.getPageSize()), wrapper);
        
        // 转换为 Page<CarApplyListItem>
        Page<CarApplyListItem> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<CarApplyListItem> itemList = buildListItemList(page.getRecords());
        resultPage.setRecords(itemList);
        
        return resultPage;
    }

    @Override
    public Page<CarApplyListItem> getAllApplicationList(CarApplyQueryRequest request, Long userId) {
        LambdaQueryWrapper<CarApplication> wrapper = new LambdaQueryWrapper<>();

        // 数据权限过滤
        applyDataScope(wrapper, userId);

        if (request.getDeptId() != null) {
            wrapper.eq(CarApplication::getDeptId, request.getDeptId());
        }
        if (StrUtil.isNotBlank(request.getApplicantName())) {
            // 关联查询申请人姓名，简化处理：先查用户再构造条件
        }

        applyQueryConditions(wrapper, request);
        wrapper.orderByDesc(CarApplication::getCreateTime);

        Page<CarApplication> page = this.page(new Page<>(request.getPageNum(), request.getPageSize()), wrapper);
        
        // 转换为 Page<CarApplyListItem>
        Page<CarApplyListItem> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<CarApplyListItem> itemList = buildListItemList(page.getRecords());
        resultPage.setRecords(itemList);
        
        return resultPage;
    }

    @Override
    public CarApplyDetailResponse getApplicationDetail(Long applyId) {
        CarApplication application = this.getById(applyId);
        if (application == null) {
            throw new BusinessException("申请不存在");
        }

        CarApplyDetailResponse response = new CarApplyDetailResponse();
        response.setId(application.getId());
        response.setTitle(application.getTitle());
        response.setStartTime(application.getStartTime());
        response.setEndTime(application.getEndTime());
        response.setReason(application.getReason());
        response.setPassengers(application.getPassengers());
        response.setDestination(application.getDestination());
        response.setVehicleType(application.getVehicleType());
        response.setVehicleTypeName(VehicleTypeEnum.getNameByCode(application.getVehicleType()));
        response.setAttachment(application.getAttachment());
        response.setApplicantId(application.getApplyBy());
        response.setDeptId(application.getDeptId());
        response.setTemplateId(application.getTemplateId());
        response.setTargetDeptId(application.getTargetDeptId());
        response.setCurrentNode(application.getCurrentNode());
        response.setStatus(application.getStatus());
        response.setStatusName(ApplyStatusEnum.getNameByCode(application.getStatus()));
        response.setIsUrgent(application.getIsUrgent());
        response.setCreateTime(application.getCreateTime());
        response.setUpdateTime(application.getUpdateTime());

        // 获取申请人信息
        SysUser applicant = userMapper.selectById(application.getApplyBy());
        if (applicant != null) {
            response.setApplicantName(applicant.getRealname());
        }

        // 获取部门信息
        SysDept dept = deptMapper.selectById(application.getDeptId());
        if (dept != null) {
            response.setDeptName(dept.getName());
        }

        // 获取目标部门信息
        if (application.getTargetDeptId() != null) {
            SysDept targetDept = deptMapper.selectById(application.getTargetDeptId());
            if (targetDept != null) {
                response.setTargetDeptName(targetDept.getName());
            }
        }

        // 获取模板信息
        ProcessTemplate template = templateMapper.selectById(application.getTemplateId());
        if (template != null) {
            response.setTemplateName(template.getName());
            response.setTemplateType(template.getType());

            if (application.getCurrentNode() != null) {
                JSONArray nodes = JSONUtil.parseArray(template.getNodeConfig());
                for (int i = 0; i < nodes.size(); i++) {
                    JSONObject node = nodes.getJSONObject(i);
                    if (application.getCurrentNode().equals(node.getInt("nodeOrder"))) {
                        response.setCurrentNodeName(node.getStr("nodeName"));
                        break;
                    }
                }
            }
        }

        // 获取当前审批人姓名
        if (StrUtil.isNotBlank(application.getCurrentApproverIds())) {
            String[] approverIdArray = application.getCurrentApproverIds().split(",");
            List<String> approverNames = new ArrayList<>();
            for (String idStr : approverIdArray) {
                if (StrUtil.isNotBlank(idStr)) {
                    SysUser user = userMapper.selectById(Long.parseLong(idStr.trim()));
                    if (user != null) {
                        approverNames.add(user.getRealname());
                    }
                }
            }
            response.setCurrentApproverNames(approverNames);
        }

        return response;
    }

    @Override
    public List<CarApplyListItem> getPendingList(Long userId) {
        List<CarApplication> applications = baseMapper.selectPendingListByUserId(userId);
        return buildListItemList(applications);
    }

    @Override
    public List<Long> getNodeApprovers(Integer templateId, Integer nodeOrder,
                                       Integer applyDeptId, Integer targetDeptId) {
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
        String dynamicType = targetNode.getStr("dynamicType");

        List<Long> result = new ArrayList<>();

        // 处理动态部门类型
        if ("target_dept".equals(dynamicType)) {
            // 目标部门审批人（跨部门用车）
            if (targetDeptId == null) {
                throw new BusinessException("跨部门用车需要指定目标部门");
            }
            return getApproversByDeptAndRole(targetDeptId, approverType, approverValueObj);
        }

        // 确定使用的部门ID
        Integer useDeptId = null;
        if ("applicant_dept".equals(dynamicType)) {
            useDeptId = applyDeptId;
        }
        // 其他情况（无dynamicType或null）：useDeptId = null，不限制部门

        // 根据配置获取审批人
        return getApproversByConfig(approverType, approverValueObj, useDeptId);
    }

    /**
     * 根据配置获取审批人（不限制部门或使用指定部门）
     */
    private List<Long> getApproversByConfig(String approverType, Object approverValueObj, Integer deptId) {
        List<Long> result = new ArrayList<>();

        if ("role".equals(approverType)) {
            List<Long> roleIds = extractRoleIds(approverValueObj);
            for (Long roleId : roleIds) {
                LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRoleId, roleId)
                        .eq(SysUser::getStatus, 1);
                if (deptId != null) {
                    wrapper.eq(SysUser::getDeptId, deptId);
                }
                List<SysUser> users = userMapper.selectList(wrapper);
                for (SysUser user : users) {
                    if (!result.contains(user.getId())) {
                        result.add(user.getId());
                    }
                }
            }
        } else if ("user".equals(approverType)) {
            result.addAll(extractUserIds(approverValueObj));
        } else if ("mixed".equals(approverType)) {
            List<Long> roleIds = extractRoleIds(approverValueObj);
            for (Long roleId : roleIds) {
                LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRoleId, roleId)
                        .eq(SysUser::getStatus, 1);
                if (deptId != null) {
                    wrapper.eq(SysUser::getDeptId, deptId);
                }
                List<SysUser> users = userMapper.selectList(wrapper);
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

    /**
     * 根据部门和角色获取审批人（用于跨部门用车）
     */
    private List<Long> getApproversByDeptAndRole(Integer deptId, String approverType, Object approverValueObj) {
        return getApproversByConfig(approverType, approverValueObj, deptId);
    }

    /**
     * 提取角色ID（小于阈值）
     */
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

    /**
     * 提取用户ID（大于等于阈值）
     */
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

    /**
     * 解析 approverValue 为对象列表
     */
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
    private Integer calculateTargetDeptId(Integer templateType, Integer currentDeptId, Integer requestTargetDeptId) {
        if (templateType == null) {
            throw new BusinessException("模板类型不能为空");
        }

        switch (templateType) {
            case 1:  // 内部用车：使用申请人所在部门
                if (currentDeptId == null) {
                    throw new BusinessException("申请人未分配部门，无法提交内部用车申请");
                }
                return currentDeptId;

            case 2:  // 跨部门用车：使用请求中传入的目标部门
                if (requestTargetDeptId == null) {
                    throw new BusinessException("跨部门用车需要指定目标部门");
                }
                return requestTargetDeptId;

            case 3:  // 长途用车：使用总公司（ID=1）
                return 1;

            default:
                throw new BusinessException("不支持的模板类型：" + templateType);
        }
    }
    private void applyQueryConditions(LambdaQueryWrapper<CarApplication> wrapper, CarApplyQueryRequest request) {
        if (request.getStatus() != null) {
            wrapper.eq(CarApplication::getStatus, request.getStatus());
        }
        if (request.getStartTime() != null) {
            wrapper.ge(CarApplication::getCreateTime, request.getStartTime());
        }
        if (request.getEndTime() != null) {
            wrapper.le(CarApplication::getCreateTime, request.getEndTime());
        }
        if (request.getTemplateId() != null) {
            wrapper.eq(CarApplication::getTemplateId, request.getTemplateId());
        }
        if (StrUtil.isNotBlank(request.getApplicantName())) {
            // 先查询匹配姓名的用户ID
            LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.like(SysUser::getRealname, request.getApplicantName())
                      .select(SysUser::getId);
            List<SysUser> users = userMapper.selectList(userWrapper);
            if (!users.isEmpty()) {
                List<Long> userIds = users.stream().map(SysUser::getId).collect(Collectors.toList());
                wrapper.in(CarApplication::getApplyBy, userIds);
            } else {
                // 如果没有匹配的用户，返回空结果
                wrapper.eq(CarApplication::getId, -1);
            }
        }
    }

    /**
     * 根据用户的 dataScope 数据权限范围对查询添加过滤条件
     */
    private void applyDataScope(LambdaQueryWrapper<CarApplication> wrapper, Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) return;
        if (user.getRoleId() == null || user.getDeptId() == null) return;

        // 超级管理员（roleId=1）不限制数据范围
        if (user.getRoleId() == 1) return;

        // 查询用户的 dataScope
        LambdaQueryWrapper<SysDeptRole> scopeWrapper = new LambdaQueryWrapper<>();
        scopeWrapper.eq(SysDeptRole::getDeptId, user.getDeptId())
                    .eq(SysDeptRole::getRoleId, user.getRoleId());
        SysDeptRole deptRole = deptRoleMapper.selectOne(scopeWrapper);
        if (deptRole == null || deptRole.getDataScope() == null) {
            // 默认为仅本人
            wrapper.eq(CarApplication::getApplyBy, userId);
            return;
        }

        switch (deptRole.getDataScope()) {
            case "self":
                wrapper.eq(CarApplication::getApplyBy, userId);
                break;
            case "dept":
                wrapper.eq(CarApplication::getDeptId, user.getDeptId());
                break;
            case "dept_and_sub":
                List<Integer> deptIds = deptService.getDeptAndSubIds(user.getDeptId());
                wrapper.in(CarApplication::getDeptId, deptIds);
                break;
            case "all":
                // 全部数据不限制
                break;
            default:
                wrapper.eq(CarApplication::getApplyBy, userId);
                break;
        }
    }

    private List<CarApplyListItem> buildListItemList(List<CarApplication> applications) {
        if (applications.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> userIds = applications.stream()
                .map(CarApplication::getApplyBy)
                .collect(Collectors.toSet());
        Map<Long, SysUser> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<SysUser> users = userMapper.selectBatchIds(userIds);
            for (SysUser user : users) {
                userMap.put(user.getId(), user);
            }
        }

        Set<Integer> deptIds = applications.stream()
                .map(CarApplication::getDeptId)
                .collect(Collectors.toSet());
        Map<Integer, SysDept> deptMap = new HashMap<>();
        if (!deptIds.isEmpty()) {
            List<SysDept> depts = deptMapper.selectBatchIds(deptIds);
            for (SysDept dept : depts) {
                deptMap.put(dept.getId(), dept);
            }
        }

        Set<Integer> templateIds = applications.stream()
                .map(CarApplication::getTemplateId)
                .collect(Collectors.toSet());
        Map<Integer, ProcessTemplate> templateMap = new HashMap<>();
        if (!templateIds.isEmpty()) {
            List<ProcessTemplate> templates = templateMapper.selectBatchIds(templateIds);
            for (ProcessTemplate template : templates) {
                templateMap.put(template.getTemplateId(), template);
            }
        }

        List<CarApplyListItem> result = new ArrayList<>();
        for (CarApplication app : applications) {
            CarApplyListItem item = new CarApplyListItem();
            item.setId(app.getId());
            item.setTitle(app.getTitle());
            item.setStatus(app.getStatus());
            item.setStatusName(ApplyStatusEnum.getNameByCode(app.getStatus()));
            item.setCurrentNode(app.getCurrentNode());
            item.setIsUrgent(app.getIsUrgent());
            item.setCreateTime(app.getCreateTime());
            item.setUpdateTime(app.getUpdateTime());

            SysUser user = userMap.get(app.getApplyBy());
            if (user != null) {
                item.setApplicantName(user.getRealname());
            }

            SysDept dept = deptMap.get(app.getDeptId());
            if (dept != null) {
                item.setDeptName(dept.getName());
            }

            ProcessTemplate template = templateMap.get(app.getTemplateId());
            if (template != null && app.getCurrentNode() != null) {
                JSONArray nodes = JSONUtil.parseArray(template.getNodeConfig());
                for (int i = 0; i < nodes.size(); i++) {
                    JSONObject node = nodes.getJSONObject(i);
                    if (app.getCurrentNode().equals(node.getInt("nodeOrder"))) {
                        item.setCurrentNodeName(node.getStr("nodeName"));
                        break;
                    }
                }
            }

            if (StrUtil.isNotBlank(app.getCurrentApproverIds())) {
                // 修改为按逗号分隔解析
                String[] approverIdArray = app.getCurrentApproverIds().split(",");
                List<String> approverNames = new ArrayList<>();
                for (String idStr : approverIdArray) {
                    if (StrUtil.isNotBlank(idStr)) {
                        SysUser approver = userMapper.selectById(Long.parseLong(idStr.trim()));
                        if (approver != null) {
                            approverNames.add(approver.getRealname());
                        }
                    }
                }
                item.setCurrentApproverNames(String.join(",", approverNames));
            }

            result.add(item);
        }
        return result;
    }

    @Override
    public Page<CarApplyListItem> getApprovedByMeList(CarApplyQueryRequest request, Long userId) {
        // 查询process_history表中process_by = userId的记录，获取apply_id列表
        LambdaQueryWrapper<ProcessHistory> historyWrapper = new LambdaQueryWrapper<>();
        historyWrapper.eq(ProcessHistory::getProcessBy, userId)
                     .select(ProcessHistory::getApplyId)
                     .groupBy(ProcessHistory::getApplyId);
        List<ProcessHistory> histories = historyMapper.selectList(historyWrapper);

        if (histories.isEmpty()) {
            return new Page<>(request.getPageNum(), request.getPageSize(), 0);
        }

        List<Long> applyIds = histories.stream()
                .map(ProcessHistory::getApplyId)
                .distinct()
                .collect(Collectors.toList());

        // 根据apply_id列表查询申请
        LambdaQueryWrapper<CarApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(CarApplication::getId, applyIds);
        applyQueryConditions(wrapper, request);
        wrapper.orderByDesc(CarApplication::getCreateTime);

        Page<CarApplication> page = this.page(new Page<>(request.getPageNum(), request.getPageSize()), wrapper);
        Page<CarApplyListItem> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        resultPage.setRecords(buildListItemList(page.getRecords()));
        return resultPage;
    }

    @Override
    public byte[] exportApplications(CarApplyQueryRequest request, Long userId) {
        // 查询数据（不分页）
        LambdaQueryWrapper<CarApplication> wrapper = new LambdaQueryWrapper<>();

        // 数据权限过滤
        applyDataScope(wrapper, userId);

        if (request.getDeptId() != null) {
            wrapper.eq(CarApplication::getDeptId, request.getDeptId());
        }

        applyQueryConditions(wrapper, request);
        wrapper.orderByDesc(CarApplication::getCreateTime);
        
        List<CarApplication> applications = this.list(wrapper);
        
        // 转换为导出DTO
        List<CarApplyExportDTO> exportList = new ArrayList<>();
        for (CarApplication app : applications) {
            CarApplyExportDTO dto = new CarApplyExportDTO();
            dto.setId(app.getId());
            dto.setTitle(app.getTitle());
            dto.setStartTime(app.getStartTime());
            dto.setEndTime(app.getEndTime());
            dto.setReason(app.getReason());
            dto.setPassengers(app.getPassengers());
            dto.setDestination(app.getDestination());
            dto.setVehicleTypeName(VehicleTypeEnum.getNameByCode(app.getVehicleType()));
            dto.setIsUrgentName(app.getIsUrgent() == 1 ? "是" : "否");
            dto.setStatusName(ApplyStatusEnum.getNameByCode(app.getStatus()));
            dto.setCreateTime(app.getCreateTime());
            dto.setUpdateTime(app.getUpdateTime());
            
            // 获取申请人姓名
            SysUser applicant = userMapper.selectById(app.getApplyBy());
            if (applicant != null) {
                dto.setApplicantName(applicant.getRealname());
            }
            
            // 获取部门名称
            SysDept dept = deptMapper.selectById(app.getDeptId());
            if (dept != null) {
                dto.setDeptName(dept.getName());
            }
            
            // 获取目标部门名称
            if (app.getTargetDeptId() != null) {
                SysDept targetDept = deptMapper.selectById(app.getTargetDeptId());
                if (targetDept != null) {
                    dto.setTargetDeptName(targetDept.getName());
                }
            }
            
            // 获取模板名称
            ProcessTemplate template = templateMapper.selectById(app.getTemplateId());
            if (template != null) {
                dto.setTemplateName(template.getName());
                
                // 获取当前节点名称
                if (app.getCurrentNode() != null) {
                    JSONArray nodes = JSONUtil.parseArray(template.getNodeConfig());
                    for (int i = 0; i < nodes.size(); i++) {
                        JSONObject node = nodes.getJSONObject(i);
                        if (app.getCurrentNode().equals(node.getInt("nodeOrder"))) {
                            dto.setCurrentNodeName(node.getStr("nodeName"));
                            break;
                        }
                    }
                }
            }
            
            // 获取当前审批人姓名
            if (StrUtil.isNotBlank(app.getCurrentApproverIds())) {
                String[] approverIdArray = app.getCurrentApproverIds().split(",");
                List<String> approverNames = new ArrayList<>();
                for (String idStr : approverIdArray) {
                    if (StrUtil.isNotBlank(idStr)) {
                        SysUser user = userMapper.selectById(Long.parseLong(idStr.trim()));
                        if (user != null) {
                            approverNames.add(user.getRealname());
                        }
                    }
                }
                dto.setCurrentApproverNames(String.join(",", approverNames));
            }
            
            exportList.add(dto);
        }
        
        // 使用EasyExcel生成Excel文件
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            EasyExcel.write(outputStream, CarApplyExportDTO.class)
                    .sheet("用车申请数据")
                    .doWrite(exportList);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("导出Excel失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void handleAbnormalApplication(Long applyId, Integer action, String reason, Long operatorId) {
        CarApplication application = this.getById(applyId);
        if (application == null) {
            throw new BusinessException("申请不存在");
        }
        
        // 校验操作类型
        if (action == null || (action != 1 && action != 2)) {
            throw new BusinessException("操作类型错误，1-强制通过，2-强制驳回");
        }
        
        // 如果是驳回，必须填写原因
        if (action == 2 && StrUtil.isBlank(reason)) {
            throw new BusinessException("驳回时必须填写原因");
        }
        
        // 只有待审批或审批中的申请才能处理异常
        if (application.getStatus() != ApplyStatusEnum.PENDING.getCode() &&
            application.getStatus() != ApplyStatusEnum.PROCESSING.getCode()) {
            throw new BusinessException("当前状态不可处理异常");
        }
        
        // 获取模板配置和当前节点名称
        ProcessTemplate template = templateMapper.selectById(application.getTemplateId());
        String currentNodeName = "未知节点";
        if (template != null && application.getCurrentNode() != null) {
            JSONArray nodes = JSONUtil.parseArray(template.getNodeConfig());
            for (int i = 0; i < nodes.size(); i++) {
                JSONObject node = nodes.getJSONObject(i);
                if (application.getCurrentNode().equals(node.getInt("nodeOrder"))) {
                    currentNodeName = node.getStr("nodeName");
                    break;
                }
            }
        }
        
        // 记录审批历史
        ProcessHistory history = new ProcessHistory();
        history.setApplyId(application.getId());
        history.setNodeOrder(application.getCurrentNode());
        history.setNodeName(currentNodeName);
        history.setProcessBy(operatorId);
        history.setAction(action);  // 1-通过，2-驳回
        history.setOpinion(StrUtil.isNotBlank(reason) ? reason : "管理员强制处理");
        history.setProcessTime(LocalDateTime.now());
        historyMapper.insert(history);
        
        if (action == 1) {
            // 强制通过 - 直接设置为审批通过
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
        } else {
            // 强制驳回
            application.setStatus(ApplyStatusEnum.REJECTED.getCode());
            application.setCurrentNode(null);
            application.setCurrentApproverIds(null);
            application.setUpdateTime(LocalDateTime.now());
            carApplicationMapper.updateById(application);
            
            // 发送通知给申请人
            notificationService.sendApprovalRejectedNotification(
                application.getApplyBy(),
                application.getTitle(),
                reason != null ? reason : "管理员强制驳回",
                application.getId()
            );
        }
    }
}