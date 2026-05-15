package com.baoying.vehicleapplication.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baoying.vehicleapplication.common.BusinessException;
import com.baoying.vehicleapplication.dto.request.DeptRoleAssignRequest;
import com.baoying.vehicleapplication.dto.request.DeptRoleRemoveRequest;
import com.baoying.vehicleapplication.dto.request.RoleRequest;
import com.baoying.vehicleapplication.dto.response.DeptRoleResponse;
import com.baoying.vehicleapplication.dto.response.RoleResponse;
import com.baoying.vehicleapplication.entity.SysDeptRole;
import com.baoying.vehicleapplication.entity.SysRole;
import com.baoying.vehicleapplication.mapper.DeptRoleMapper;
import com.baoying.vehicleapplication.mapper.RoleMapper;
import com.baoying.vehicleapplication.service.RoleService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, SysRole> implements RoleService {

    @Autowired
    private DeptRoleMapper deptRoleMapper;

    @Override
    @Transactional
    public void addRole(RoleRequest request) {
        // 检查角色名称是否已存在
        if (checkRoleNameExist(request.getName(), null)) {
            throw new BusinessException("角色名称已存在");
        }
        
        SysRole role = new SysRole();
        BeanUtils.copyProperties(request, role);
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        this.save(role);
    }

    @Override
    @Transactional
    public void updateRole(RoleRequest request) {
        if (request.getId() == null) {
            throw new BusinessException("角色ID不能为空");
        }
        
        SysRole existRole = this.getById(request.getId());
        if (existRole == null) {
            throw new BusinessException("角色不存在");
        }
        
        // 检查角色名称是否已存在（排除自身）
        if (checkRoleNameExist(request.getName(), request.getId())) {
            throw new BusinessException("角色名称已存在");
        }
        
        SysRole role = new SysRole();
        BeanUtils.copyProperties(request, role);
        this.updateById(role);
    }

    @Override
    @Transactional
    public void deleteRole(Integer id) {
        SysRole role = this.getById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        
        // 检查是否有部门关联该角色
        Integer deptCount = baseMapper.countDeptByRoleId(id);
        if (deptCount > 0) {
            throw new BusinessException("该角色已被 " + deptCount + " 个部门关联，请先解除关联");
        }
        
        // 检查是否有账号使用该角色
        Integer userCount = baseMapper.countUserByRoleId(id);
        if (userCount > 0) {
            throw new BusinessException("该角色已被 " + userCount + " 个账号使用，请先修改账号的角色");
        }
        
        this.removeById(id);
    }

    @Override
    @Transactional
    public void updateStatus(Integer id, Integer status) {
        SysRole role = this.getById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }

        role.setStatus(status);
        this.updateById(role);
    }

    @Override
    public List<RoleResponse> listRoles(String name, Integer status) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            wrapper.like(SysRole::getName, name);
        }
        if (status != null) {
            wrapper.eq(SysRole::getStatus, status);
        }
        wrapper.orderByAsc(SysRole::getId);
        
        List<SysRole> roles = this.list(wrapper);
        List<RoleResponse> result = new ArrayList<>();
        for (SysRole role : roles) {
            RoleResponse resp = new RoleResponse();
            BeanUtils.copyProperties(role, resp);
            resp.setUserCount(baseMapper.countUserByRoleId(role.getId()));
            resp.setDeptCount(baseMapper.countDeptByRoleId(role.getId()));
            result.add(resp);
        }
        return result;
    }

    @Override
    public RoleResponse getRoleDetail(Integer id) {
        SysRole role = this.getById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        
        RoleResponse resp = new RoleResponse();
        BeanUtils.copyProperties(role, resp);
        resp.setUserCount(baseMapper.countUserByRoleId(id));
        resp.setDeptCount(baseMapper.countDeptByRoleId(id));
        return resp;
    }

    @Override
    @Transactional
    public void assignRoleToDept(DeptRoleAssignRequest request) {
        // 检查角色是否存在
        SysRole role = this.getById(request.getRoleId());
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        
        // 检查部门是否存在（这里假设有DeptService）
        // 检查是否已关联
        LambdaQueryWrapper<SysDeptRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDeptRole::getDeptId, request.getDeptId())
               .eq(SysDeptRole::getRoleId, request.getRoleId());
        if (deptRoleMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("该角色已关联到此部门");
        }
        
        SysDeptRole deptRole = new SysDeptRole();
        deptRole.setDeptId(request.getDeptId());
        deptRole.setRoleId(request.getRoleId());
        deptRole.setDataScope(request.getDataScope() == null ? "self" : request.getDataScope());
        deptRoleMapper.insert(deptRole);
    }

    @Override
    @Transactional
    public void removeRoleFromDept(DeptRoleRemoveRequest request) {
        // 检查该部门下是否有账号使用此角色
        Integer userCount = deptRoleMapper.countUserByDeptAndRole(request.getDeptId(), request.getRoleId());
        if (userCount > 0) {
            throw new BusinessException("该部门下有 " + userCount + " 个账号正在使用此角色，请先修改这些账号的角色");
        }
        
        LambdaQueryWrapper<SysDeptRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDeptRole::getDeptId, request.getDeptId())
               .eq(SysDeptRole::getRoleId, request.getRoleId());
        deptRoleMapper.delete(wrapper);
    }

    @Override
    @Transactional
    public void updateDeptRoleDataScope(Integer deptId, Integer roleId, String dataScope) {
        LambdaQueryWrapper<SysDeptRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDeptRole::getDeptId, deptId)
               .eq(SysDeptRole::getRoleId, roleId);
        SysDeptRole deptRole = deptRoleMapper.selectOne(wrapper);
        if (deptRole == null) {
            throw new BusinessException("该部门未关联此角色");
        }
        deptRole.setDataScope(dataScope == null ? "self" : dataScope);
        deptRoleMapper.update(deptRole, wrapper);
    }

    @Override
    public List<DeptRoleResponse> getDeptRoles(Integer deptId) {
        List<SysDeptRole> deptRoles = deptRoleMapper.selectList(
            new LambdaQueryWrapper<SysDeptRole>().eq(SysDeptRole::getDeptId, deptId)
        );
        
        List<DeptRoleResponse> result = new ArrayList<>();
        for (SysDeptRole deptRole : deptRoles) {
            SysRole role = this.getById(deptRole.getRoleId());
            if (role != null && role.getStatus() == 1) {
                DeptRoleResponse resp = new DeptRoleResponse();
                resp.setRoleId(role.getId());
                resp.setRoleName(role.getName());
                resp.setDataScope(deptRole.getDataScope());
                resp.setUserCount(deptRoleMapper.countUserByDeptAndRole(deptId, role.getId()));
                result.add(resp);
            }
        }
        return result;
    }

    @Override
    public List<SysRole> getAvailableRolesForDept(Integer deptId) {
        // 获取该部门已关联的角色ID
        List<SysDeptRole> existing = deptRoleMapper.selectList(
            new LambdaQueryWrapper<SysDeptRole>().eq(SysDeptRole::getDeptId, deptId)
        );
        List<Integer> existingRoleIds = existing.stream()
                .map(SysDeptRole::getRoleId)
                .collect(Collectors.toList());
        
        // 查询所有角色，排除已关联的
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getStatus, 1);
        if (!existingRoleIds.isEmpty()) {
            wrapper.notIn(SysRole::getId, existingRoleIds);
        }
        wrapper.orderByAsc(SysRole::getId);
        
        return this.list(wrapper);
    }
    
    private boolean checkRoleNameExist(String name, Integer excludeId) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getName, name);
        if (excludeId != null) {
            wrapper.ne(SysRole::getId, excludeId);
        }
        return this.count(wrapper) > 0;
    }
}