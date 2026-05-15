package com.baoying.vehicleapplication.controller;

import com.baoying.vehicleapplication.annotation.RequirePermission;
import com.baoying.vehicleapplication.common.Result;
import com.baoying.vehicleapplication.dto.request.TemplateRequest;
import com.baoying.vehicleapplication.dto.response.TemplateListItem;
import com.baoying.vehicleapplication.dto.response.TemplateResponse;
import com.baoying.vehicleapplication.service.ProcessTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

import static com.baoying.vehicleapplication.utils.CurrentUserUtils.getCurrentUserId;

@RestController
@RequestMapping("/flow/template")
public class ProcessTemplateController {

    @Autowired
    private ProcessTemplateService templateService;


    /**
     * 新增模板（仅超级管理员）
     */
    @RequirePermission(roles = {1})
    @PostMapping("/add")
    public Result<Void> add(@Valid @RequestBody TemplateRequest request) {
        Long currentUserId = getCurrentUserId();
        templateService.addTemplate(request, currentUserId);
        return Result.success();
    }

    /**
     * 编辑模板（仅超级管理员）
     */
    @RequirePermission(roles = {1})
    @PutMapping("/update")
    public Result<Void> update(@Valid @RequestBody TemplateRequest request) {
        templateService.updateTemplate(request);
        return Result.success();
    }

    /**
     * 删除模板（仅超级管理员）
     */
    @RequirePermission(roles = {1})
    @DeleteMapping("/delete/{templateId}")
    public Result<Void> delete(@PathVariable Integer templateId) {
        templateService.deleteTemplate(templateId);
        return Result.success();
    }

    /**
     * 启用/禁用模板（仅超级管理员）
     */
    @RequirePermission(roles = {1})
    @PutMapping("/status/{templateId}")
    public Result<Void> updateStatus(@PathVariable Integer templateId, @RequestParam Integer status) {
        templateService.updateStatus(templateId, status);
        return Result.success();
    }

    /**
     * 模板列表（所有登录用户）
     */
    @RequirePermission(checkRole = false)
    @GetMapping("/list")
    public Result<List<TemplateListItem>> list(@RequestParam(required = false) String name,
                                                @RequestParam(required = false) Integer status) {
        return Result.success(templateService.listTemplates(name, status));
    }

    /**
     * 模板详情（所有登录用户）
     */
    @RequirePermission(checkRole = false)
    @GetMapping("/detail/{templateId}")
    public Result<TemplateResponse> detail(@PathVariable Integer templateId) {
        return Result.success(templateService.getTemplateDetail(templateId));
    }
}