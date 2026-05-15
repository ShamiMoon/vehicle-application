package com.baoying.vehicleapplication.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baoying.vehicleapplication.dto.request.DeptRequest;
import com.baoying.vehicleapplication.dto.request.DeptSortItem;
import com.baoying.vehicleapplication.dto.response.DeptDetailResponse;
import com.baoying.vehicleapplication.dto.response.DeptTreeResponse;
import com.baoying.vehicleapplication.entity.SysDept;
import com.baoying.vehicleapplication.entity.SysUser;

import java.util.List;

public interface DeptService extends IService<SysDept> {

    /**
     * 新增部门
     */
    void addDept(DeptRequest request, Long createBy);

    /**
     * 编辑部门
     */
    void updateDept(DeptRequest request);

    /**
     * 删除部门（需检查是否有子部门和账号）
     * @param targetDeptId 可选，目标部门ID，不为空时先将账号转移到该部门再删除
     */
    void deleteDept(Integer id, Integer targetDeptId);

    /**
     * 启用/禁用部门（禁用时同步禁用子部门）
     */
    void updateStatus(Integer id, Integer status);

    /**
     * 获取部门树形列表
     */
    List<DeptTreeResponse> getDeptTree();

    /**
     * 获取部门详情
     */
    DeptDetailResponse getDeptDetail(Integer id);

    /**
     * 获取部门下账号数量
     */
    Long getUserCountByDeptId(Integer deptId);

    /**
     * 获取部门下的账号列表
     */
    List<SysUser> getUsersByDeptId(Integer deptId);

    /**
     * 检查部门名称是否已存在（排除自身）
     */
    boolean isDeptNameExist(String name, Integer excludeId);

    /**
     * 检查父部门是否存在且启用
     */
    void checkParentDept(Integer parentId);
    /**
     * 批量转移部门下的账号到目标部门
     */
    void transferUsers(Integer sourceDeptId, Integer targetDeptId);
    /**
     * 批量更新部门排序
     */
    void batchUpdateSort(List<DeptSortItem> items);

    List<SysDept> getList();

    /**
     * 获取部门及其所有下级部门的ID列表（递归）
     */
    List<Integer> getDeptAndSubIds(Integer deptId);
}