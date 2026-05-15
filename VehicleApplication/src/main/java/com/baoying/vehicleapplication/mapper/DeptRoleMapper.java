package com.baoying.vehicleapplication.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baoying.vehicleapplication.entity.SysDeptRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DeptRoleMapper extends BaseMapper<SysDeptRole> {
    
    @Select("SELECT COUNT(*) FROM sys_user WHERE dept_id = #{deptId} AND role_id = #{roleId}")
    Integer countUserByDeptAndRole(@Param("deptId") Integer deptId, @Param("roleId") Integer roleId);
    
    @Select("SELECT r.*, dr.data_scope FROM sys_role r " +
            "INNER JOIN sys_dept_role dr ON r.id = dr.role_id " +
            "WHERE dr.dept_id = #{deptId} AND r.status = 1 " +
            "ORDER BY r.id")
    List<Object> selectRolesByDeptId(Integer deptId);
}