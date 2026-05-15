package com.baoying.vehicleapplication.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baoying.vehicleapplication.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DeptMapper extends BaseMapper<SysDept> {
    
    /**
     * 获取所有启用的部门
     */
    @Select("SELECT * FROM sys_dept WHERE status = 1 ORDER BY sort ASC")
    List<SysDept> selectAllEnabled();
    
    /**
     * 根据上级部门ID查询子部门列表
     */
    @Select("SELECT * FROM sys_dept WHERE parent_id = #{parentId} ORDER BY sort ASC")
    List<SysDept> selectByParentId(Integer parentId);
    
    /**
     * 递归查询所有子部门ID（MySQL递归查询）
     */
    @Select("WITH RECURSIVE dept_cte AS (" +
            "  SELECT id, parent_id FROM sys_dept WHERE id = #{deptId} " +
            "  UNION ALL " +
            "  SELECT d.id, d.parent_id FROM sys_dept d " +
            "  INNER JOIN dept_cte c ON d.parent_id = c.id" +
            ") SELECT id FROM dept_cte")
    List<Integer> selectAllChildIds(Integer deptId);
    
    /**
     * 禁用部门（包括子部门）
     */
    @Update("UPDATE sys_dept SET status = #{status} WHERE id IN " +
            "(WITH RECURSIVE dept_cte AS (" +
            "  SELECT id FROM sys_dept WHERE id = #{deptId} " +
            "  UNION ALL " +
            "  SELECT d.id FROM sys_dept d " +
            "  INNER JOIN dept_cte c ON d.parent_id = c.id" +
            ") SELECT id FROM dept_cte)")
    void updateStatusWithChildren(@Param("deptId") Integer deptId,@Param("status") Integer status);
}