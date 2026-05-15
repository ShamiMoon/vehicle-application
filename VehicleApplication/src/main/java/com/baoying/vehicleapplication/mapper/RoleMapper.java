package com.baoying.vehicleapplication.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baoying.vehicleapplication.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RoleMapper extends BaseMapper<SysRole> {
    
    @Select("SELECT COUNT(*) FROM sys_user WHERE role_id = #{roleId}")
    Integer countUserByRoleId(Integer roleId);
    
    @Select("SELECT COUNT(*) FROM sys_dept_role WHERE role_id = #{roleId}")
    Integer countDeptByRoleId(Integer roleId);
}