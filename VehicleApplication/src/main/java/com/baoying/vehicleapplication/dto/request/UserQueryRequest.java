package com.baoying.vehicleapplication.dto.request;

import lombok.Data;

/**
 * 用户列表查询请求
 */
@Data
public class UserQueryRequest {
    
    /**
     * 用户名（模糊搜索）
     */
    private String username;
    
    /**
     * 真实姓名（模糊搜索）
     */
    private String realname;
    
    /**
     * 部门ID
     */
    private Integer deptId;
    
    /**
     * 角色ID
     */
    private Integer roleId;
    
    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;
    
    /**
     * 页码（默认1）
     */
    private Integer pageNum = 1;
    
    /**
     * 每页大小（默认10）
     */
    private Integer pageSize = 10;
}
