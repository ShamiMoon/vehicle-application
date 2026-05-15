package com.baoying.vehicleapplication.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baoying.vehicleapplication.common.BusinessException;
import com.baoying.vehicleapplication.dto.request.DeptRequest;
import com.baoying.vehicleapplication.dto.request.DeptSortItem;
import com.baoying.vehicleapplication.dto.response.DeptDetailResponse;
import com.baoying.vehicleapplication.dto.response.DeptTreeResponse;
import com.baoying.vehicleapplication.entity.SysDept;
import com.baoying.vehicleapplication.entity.SysDeptRole;
import com.baoying.vehicleapplication.entity.SysUser;
import com.baoying.vehicleapplication.mapper.DeptMapper;
import com.baoying.vehicleapplication.mapper.DeptRoleMapper;
import com.baoying.vehicleapplication.mapper.UserMapper;
import com.baoying.vehicleapplication.service.DeptService;
import com.baoying.vehicleapplication.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeptServiceImpl extends ServiceImpl<DeptMapper, SysDept> implements DeptService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DeptRoleMapper deptRoleMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private DeptMapper deptMapper;

    @Override
    @Transactional
    public void addDept(DeptRequest request, Long createBy) {
        // 1. 校验部门名称是否重复
        if (isDeptNameExist(request.getName(), null)) {
            throw new BusinessException("部门名称已存在");
        }

        // 2. 校验父部门是否存在
        if (request.getParentId() != null && request.getParentId() != 0) {
            checkParentDept(request.getParentId());
        }

        // 3. 校验同级部门排序值是否重复（新增时需要检查）
        Integer parentId = request.getParentId() != null ? request.getParentId() : 0;
        Integer sort = request.getSort() != null ? request.getSort() : 0;
        checkSortUnique(parentId, sort, null);

        // 4. 创建部门（原有逻辑）
        SysDept dept = new SysDept();
        BeanUtils.copyProperties(request, dept);

        if (dept.getParentId() == null) {
            dept.setParentId(0);
        }
        if (dept.getStatus() == null) {
            dept.setStatus(1);
        }
        if (dept.getSort() == null) {
            dept.setSort(0);
        }
        dept.setCreateBy(createBy);

        this.save(dept);
    }

    @Override
    @Transactional
    public void updateDept(DeptRequest request) {
        // 1. 检查部门是否存在
        SysDept existDept = this.getById(request.getId());
        if (existDept == null) {
            throw new BusinessException("部门不存在");
        }

        // 2. 校验部门名称是否重复（排除自身）
        if (isDeptNameExist(request.getName(), request.getId())) {
            throw new BusinessException("部门名称已存在");
        }

        // 3. 确定最终的父部门ID和排序值
        Integer finalParentId = request.getParentId() != null ? request.getParentId() : existDept.getParentId();
        Integer finalSort = request.getSort() != null ? request.getSort() : existDept.getSort();

        // 4. 校验父部门
        if (finalParentId != null && finalParentId != 0) {
            if (finalParentId.equals(request.getId())) {
                throw new BusinessException("上级部门不能是自身");
            }
            List<Integer> childIds = baseMapper.selectAllChildIds(request.getId());
            if (childIds.contains(finalParentId)) {
                throw new BusinessException("上级部门不能是当前部门的子部门");
            }
            checkParentDept(finalParentId);
        }

        // 5. 校验同级部门排序值是否重复（排除自身）
        checkSortUnique(finalParentId, finalSort, request.getId());

        // 6. 更新部门
        SysDept dept = new SysDept();
        BeanUtils.copyProperties(request, dept);
        this.updateById(dept);
    }

    /**
     * 检查同级部门排序值是否唯一
     * @param parentId 父部门ID
     * @param sort 排序值
     * @param excludeId 排除的部门ID（编辑时传入自身ID）
     */
    private void checkSortUnique(Integer parentId, Integer sort, Integer excludeId) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDept::getParentId, parentId)
                .eq(SysDept::getSort, sort);
        if (excludeId != null) {
            wrapper.ne(SysDept::getId, excludeId);
        }
        if (this.count(wrapper) > 0) {
            throw new BusinessException("同级部门中排序值已存在");
        }
    }

    @Override
    @Transactional
    public void deleteDept(Integer id, Integer targetDeptId) {
        // 1. 检查部门是否存在
        SysDept dept = this.getById(id);
        if (dept == null) {
            throw new BusinessException("部门不存在");
        }

        // 2. 检查是否有子部门
        long childCount = this.count(new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException("部门下存在子部门，请先删除子部门");
        }

        // 3. 检查部门下是否有账号
        Long userCount = getUserCountByDeptId(id);
        if (userCount > 0) {
            if (targetDeptId != null) {
                transferUsers(id, targetDeptId);
            } else {
                throw new BusinessException("部门下存在账号，请先将账号转移或删除");
            }
        }

        // 4. 删除部门角色关联
        deptRoleMapper.delete(new LambdaQueryWrapper<SysDeptRole>().eq(SysDeptRole::getDeptId, id));

        // 5. 删除部门
        this.removeById(id);
    }

    @Override
    @Transactional
    public void updateStatus(Integer id, Integer status) {
        // 1. 检查部门是否存在
        SysDept dept = this.getById(id);
        if (dept == null) {
            throw new BusinessException("部门不存在");
        }

        if (status == 0) {
            // 禁用：递归禁用所有子部门
            baseMapper.updateStatusWithChildren(id, 0);
        } else if (status == 1) {
            // 启用：只启用当前部门，检查父部门状态
            if (dept.getParentId() != null && dept.getParentId() != 0) {
                SysDept parentDept = this.getById(dept.getParentId());
                if (parentDept == null || parentDept.getStatus() != 1) {
                    throw new BusinessException("上级部门未启用，无法启用当前部门");
                }
            }
            dept.setStatus(1);
            this.updateById(dept);
        }
    }

    @Override
    public List<DeptTreeResponse> getDeptTree() {
        // 1. 查询所有部门
        List<SysDept> allDepts = this.list(new LambdaQueryWrapper<SysDept>()
                .orderByAsc(SysDept::getSort));

        // 2. 转换为TreeResponse
        List<DeptTreeResponse> treeList = allDepts.stream().map(dept -> {
            DeptTreeResponse resp = new DeptTreeResponse();
            BeanUtils.copyProperties(dept, resp);
            resp.setChildren(new ArrayList<>());
            // 获取创建人姓名
            if (dept.getCreateBy() != null) {
                try {
                    SysUser createUser = userService.getById(dept.getCreateBy());
                    if (createUser != null) {
                        resp.setCreateByName(createUser.getRealname());
                    }
                } catch (Exception e) {
                    resp.setCreateByName("未知");
                }
            }
            return resp;
        }).collect(Collectors.toList());

        // 3. 构建树形结构
        return buildDeptTree(treeList, 0);
    }
    @Override
    public List<SysDept> getList() {
        return deptMapper.selectAllEnabled();
    }
    @Override
    public DeptDetailResponse getDeptDetail(Integer id) {
        SysDept dept = this.getById(id);
        if (dept == null) {
            throw new BusinessException("部门不存在");
        }

        DeptDetailResponse response = new DeptDetailResponse();
        BeanUtils.copyProperties(dept, response);

        // 获取上级部门名称
        if (dept.getParentId() != null && dept.getParentId() != 0) {
            SysDept parentDept = this.getById(dept.getParentId());
            if (parentDept != null) {
                response.setParentName(parentDept.getName());
            }
        } else {
            response.setParentName("顶级部门");
        }

        // 获取创建人姓名
        if (dept.getCreateBy() != null) {
            SysUser createUser = userService.getById(dept.getCreateBy());
            if (createUser != null) {
                response.setCreateByName(createUser.getRealname());
            }
        }

        // 获取部门下账号数量
        response.setUserCount(getUserCountByDeptId(id).intValue());

        return response;
    }

    @Override
    public Long getUserCountByDeptId(Integer deptId) {
        List<Integer> allDeptIds = baseMapper.selectAllChildIds(deptId);
        allDeptIds.add(deptId);

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysUser::getDeptId, allDeptIds);
        return userMapper.selectCount(wrapper);
    }

    @Override
    public List<SysUser> getUsersByDeptId(Integer deptId) {
        List<Integer> allDeptIds = baseMapper.selectAllChildIds(deptId);
        allDeptIds.add(deptId);

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysUser::getDeptId, allDeptIds)
                .orderByAsc(SysUser::getCreateTime);
        return userMapper.selectList(wrapper);
    }

    @Override
    public boolean isDeptNameExist(String name, Integer excludeId) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDept::getName, name);
        if (excludeId != null) {
            wrapper.ne(SysDept::getId, excludeId);
        }
        return this.count(wrapper) > 0;
    }

    @Override
    public void checkParentDept(Integer parentId) {
        if (parentId == 0) {
            return;
        }
        SysDept parentDept = this.getById(parentId);
        if (parentDept == null) {
            throw new BusinessException("上级部门不存在");
        }
        if (parentDept.getStatus() != 1) {
            throw new BusinessException("上级部门已被禁用，无法添加子部门");
        }
    }

    /**
     * 递归构建部门树
     */
    private List<DeptTreeResponse> buildDeptTree(List<DeptTreeResponse> list, Integer parentId) {
        List<DeptTreeResponse> result = new ArrayList<>();
        for (DeptTreeResponse node : list) {
            if (node.getParentId() != null && node.getParentId().equals(parentId)) {
                node.setChildren(buildDeptTree(list, node.getId()));
                result.add(node);
            }
        }
        return result;
    }
    @Override
    @Transactional
    public void transferUsers(Integer sourceDeptId, Integer targetDeptId) {
        // 1. 检查目标部门是否存在
        SysDept targetDept = this.getById(targetDeptId);
        if (targetDept == null) {
            throw new BusinessException("目标部门不存在");
        }

        // 2. 更新账号的部门ID
        SysUser updateUser = new SysUser();
        updateUser.setDeptId(targetDeptId);
        userMapper.update(updateUser,
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeptId, sourceDeptId));
    }
    @Override
    @Transactional
    public void batchUpdateSort(List<DeptSortItem> items) {
        for (DeptSortItem item : items) {
            SysDept dept = new SysDept();
            dept.setId(item.getId());
            dept.setSort(item.getSort());
            this.updateById(dept);
        }
    }

    @Override
    public List<Integer> getDeptAndSubIds(Integer deptId) {
        List<Integer> result = new ArrayList<>();
        result.add(deptId);
        // 递归查询下级部门
        List<SysDept> allDepts = this.list();
        collectChildIds(allDepts, deptId, result);
        return result;
    }

    private void collectChildIds(List<SysDept> allDepts, Integer parentId, List<Integer> result) {
        for (SysDept dept : allDepts) {
            if (parentId.equals(dept.getParentId())) {
                result.add(dept.getId());
                collectChildIds(allDepts, dept.getId(), result);
            }
        }
    }

}