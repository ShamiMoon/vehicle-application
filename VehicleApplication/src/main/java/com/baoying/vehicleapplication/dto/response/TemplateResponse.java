package com.baoying.vehicleapplication.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TemplateResponse {
    private Integer templateId;
    private String name;
    private String description;
    private Integer type;
    private String typeName;        // 内部用车/跨部门用车/长途用车
    private List<NodeConfig> nodeConfig;
    private Integer status;
    private String createByName;    // 创建人姓名
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    @Data
    public static class NodeConfig {
        private Integer nodeOrder;
        private String nodeName;
        private String approverType;
        private List<Long> approverValue;
        private String approveType;
        private Integer timeoutHours;
        private String rejectRule;
    }
}