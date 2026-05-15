import request from './request'

export function addRole(data) {
  return request.post('/org/role/add', data)
}

export function updateRole(data) {
  return request.put('/org/role/update', data)
}

export function deleteRole(id) {
  return request.delete(`/org/role/delete/${id}`)
}

export function getRoleList() {
  return request.get('/org/role/list')
}

export function updateRoleStatus(id, status) {
  return request.put(`/org/role/status/${id}/${status}`)
}

// ===== 部门-角色关联 =====

export function assignRoleToDept(data) {
  return request.post('/org/role/dept/assign', data)
}

export function removeRoleFromDept(data) {
  return request.delete('/org/role/dept/remove', { data })
}

export function getDeptRoles(deptId) {
  return request.get(`/org/role/dept/roles/${deptId}`)
}

export function getAvailableRoles(deptId) {
  return request.get(`/org/role/dept/available/${deptId}`)
}

export function updateDeptRoleDataScope(deptId, roleId, dataScope) {
  return request.put('/org/role/dept/data-scope', null, { params: { deptId, roleId, dataScope } })
}
