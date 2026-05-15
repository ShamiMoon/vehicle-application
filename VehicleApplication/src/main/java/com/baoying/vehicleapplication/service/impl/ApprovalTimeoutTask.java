package com.baoying.vehicleapplication.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baoying.vehicleapplication.common.ApplyStatusEnum;
import com.baoying.vehicleapplication.entity.CarApplication;
import com.baoying.vehicleapplication.entity.SysMessage;
import com.baoying.vehicleapplication.entity.SysUser;
import com.baoying.vehicleapplication.mapper.CarApplicationMapper;
import com.baoying.vehicleapplication.mapper.SysMessageMapper;
import com.baoying.vehicleapplication.mapper.UserMapper;
import com.baoying.vehicleapplication.service.EmailService;
import com.baoying.vehicleapplication.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 审批超时提醒定时任务
 * 每分钟检查一次待审批/审批中的申请，若当前节点超过配置的超时时间则发送提醒
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalTimeoutTask {

    private final CarApplicationMapper carApplicationMapper;
    private final UserMapper userMapper;
    private final SysMessageMapper sysMessageMapper;
    private final MessageService messageService;
    @Autowired(required = false)
    private EmailService emailService;

    @Scheduled(fixedRate = 60_000) // 每分钟执行一次
    public void checkTimeout() {
        log.debug("开始检查审批超时...");

        // 1. 查询所有待审批和审批中的申请
        LambdaQueryWrapper<CarApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(CarApplication::getStatus, ApplyStatusEnum.PENDING.getCode(), ApplyStatusEnum.PROCESSING.getCode());
        List<CarApplication> applications = carApplicationMapper.selectList(wrapper);

        for (CarApplication app : applications) {
            try {
                checkApplicationTimeout(app);
            } catch (Exception e) {
                log.error("检查申请 {} 超时失败: {}", app.getId(), e.getMessage(), e);
            }
        }
    }

    private void checkApplicationTimeout(CarApplication app) {
        if (app.getUpdateTime() == null) return;

        // 2. 获取模板节点配置（优先使用快照，再回退到模板当前配置）
        String configJson = app.getNodeConfigSnapshot();
        if (StrUtil.isBlank(configJson)) return;

        JSONArray nodes = JSONUtil.parseArray(configJson);
        JSONObject currentNodeConfig = null;
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            if (app.getCurrentNode() != null && app.getCurrentNode().equals(node.getInt("nodeOrder"))) {
                currentNodeConfig = node;
                break;
            }
        }
        if (currentNodeConfig == null) return;

        // 3. 检查超时时间配置
        Integer timeoutHours = currentNodeConfig.getInt("timeoutHours");
        if (timeoutHours == null || timeoutHours <= 0) return;

        // 4. 计算是否超时
        long elapsedMinutes = ChronoUnit.MINUTES.between(app.getUpdateTime(), LocalDateTime.now());
        if (elapsedMinutes < timeoutHours * 60L) return; // 未超时

        // 5. 检查是否已经发送过超时提醒（避免重复发送）
        if (hasTimeoutNotificationSent(app.getId())) return;

        // 6. 发送超时提醒给当前审批人
        String currentApproverIds = app.getCurrentApproverIds();
        if (StrUtil.isBlank(currentApproverIds)) return;

        String nodeName = currentNodeConfig.getStr("nodeName", "未知节点");
        String title = "审批超时提醒";
        String content = String.format("您的用车申请【%s】在【%s】节点已超过 %d 小时未审批，请及时处理。",
                app.getTitle(), nodeName, timeoutHours);

        String[] approverIdArr = currentApproverIds.split(",");
        for (String idStr : approverIdArr) {
            try {
                Long approverId = Long.parseLong(idStr.trim());
                messageService.sendMessage(approverId, title, content, 5, app.getId());

                // 同时发送邮件通知（如果审批人开启了邮件通知）
                SysUser approver = userMapper.selectById(approverId);
                if (approver != null && approver.getEmailNotify() != null && approver.getEmailNotify() == 1
                        && StrUtil.isNotBlank(approver.getEmail())) {
                    sendTimeoutEmail(approver, app, nodeName, timeoutHours);
                }
            } catch (NumberFormatException e) {
                log.warn("解析审批人ID失败: {}", idStr);
            }
        }

        log.info("已发送审批超时提醒 - 申请ID: {}, 节点: {}, 超时: {}小时", app.getId(), nodeName, timeoutHours);
    }

    /**
     * 检查是否已经发送过超时提醒
     */
    private boolean hasTimeoutNotificationSent(Long applyId) {
        LambdaQueryWrapper<SysMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMessage::getApplyId, applyId)
               .eq(SysMessage::getMessageType, 5);
        return sysMessageMapper.selectCount(wrapper) > 0;
    }

    private void sendTimeoutEmail(SysUser user, CarApplication app, String nodeName, Integer timeoutHours) {
        if (emailService == null) return;
        try {
            String subject = "【用车申请系统】审批超时提醒";
            String content = String.format(
                    "您好，用车申请【%s】在【%s】节点已超过 %d 小时未审批，请及时登录系统处理。",
                    app.getTitle(), nodeName, timeoutHours);
            emailService.sendApprovalEmail(user.getEmail(), subject, content);
        } catch (Exception e) {
            log.warn("发送超时邮件失败: {}", e.getMessage());
        }
    }
}
