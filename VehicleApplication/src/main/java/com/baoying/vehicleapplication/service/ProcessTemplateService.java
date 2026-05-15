package com.baoying.vehicleapplication.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baoying.vehicleapplication.dto.request.TemplateRequest;
import com.baoying.vehicleapplication.dto.response.TemplateListItem;
import com.baoying.vehicleapplication.dto.response.TemplateResponse;
import com.baoying.vehicleapplication.entity.ProcessTemplate;

import java.util.List;

public interface ProcessTemplateService extends IService<ProcessTemplate> {
    
    /** 新增模板 */
    void addTemplate(TemplateRequest request, Long createBy);
    
    /** 编辑模板 */
    void updateTemplate(TemplateRequest request);
    
    /** 删除模板（检查是否有未完成的申请） */
    void deleteTemplate(Integer templateId);
    
    /** 启用/禁用模板 */
    void updateStatus(Integer templateId, Integer status);
    
    /** 模板列表 */
    List<TemplateListItem> listTemplates(String name, Integer status);
    
    /** 模板详情 */
    TemplateResponse getTemplateDetail(Integer templateId);
    
    /** 检查模板名称是否存在 */
    boolean isTemplateNameExist(String name, Integer excludeId);
}