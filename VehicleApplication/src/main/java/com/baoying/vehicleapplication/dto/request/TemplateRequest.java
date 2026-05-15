// TemplateRequest.java（新增/编辑模板）
package com.baoying.vehicleapplication.dto.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class TemplateRequest {
    private Integer templateId;     // 编辑时传入
    
    @NotBlank(message = "模板名称不能为空")
    private String name;
    
    private String description;
    
    @NotNull(message = "流程类型不能为空")
    private Integer type;           // 1内部用车 2跨部门用车 3长途用车
    
    @NotNull(message = "节点配置不能为空")
    private List<NodeConfig> nodeConfig;
    
    private Integer status;         // 1启用 0禁用
    
    // 节点配置内部类
    @Data
    public static class NodeConfig {
        @NotNull(message = "节点顺序不能为空")
        private Integer nodeOrder;
        
        @NotBlank(message = "节点名称不能为空")
        private String nodeName;
        
        @NotBlank(message = "审批人类型不能为空")
        private String approverType;    // user / role
        
        @NotNull(message = "审批人值不能为空")
        private List<Long> approverValue;  // 用户ID列表 或 角色ID列表
        
        @NotBlank(message = "审批方式不能为空")
        private String approveType;        // single / all
        
        private Integer timeoutHours;       // 超时时间（小时）
        
        private String rejectRule;          // 驳回规则说明
    }
}