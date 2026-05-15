import request from './request'

export function addDept(data) {
  return request.post('/org/dept/add', data)
}

export function updateDept(data) {
  return request.put('/org/dept/update', data)
}

export function deleteDept(id, targetDeptId) {
  const params = targetDeptId ? { targetDeptId } : {}
  return request.delete(`/org/dept/delete/${id}`, { params })
}

export function getDeptTree() {
  return request.get('/org/dept/tree')
}

export function getDeptDetail(id) {
  return request.get(`/org/dept/detail/${id}`)
}

export function getDeptList() {
  return request.get('/org/dept/list')
}

export function updateDeptStatus(id, status) {
  return request.put(`/org/dept/status/${id}/${status}`)
}

export function getDeptUserCount(deptId) {
  return request.get(`/org/dept/user-count/${deptId}`)
}
