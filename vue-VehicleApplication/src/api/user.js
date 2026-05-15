import request from './request'

export function addUser(data) {
  return request.post('/org/user/create', data)
}

export function updateUser(data) {
  return request.put('/org/user/update', data)
}

export function deleteUser(id) {
  return request.delete(`/org/user/delete/${id}`)
}

export function getUserList(params) {
  return request.get('/org/user/list', { params })
}

export function getUserDetail(id) {
  return request.get(`/org/user/detail/${id}`)
}

export function resetPassword(id) {
  return request.put('/org/user/reset-pwd', { id })
}

export function updateProfile(data) {
  return request.put('/org/user/profile', data)
}

export function changePassword(data) {
  return request.put('/org/user/change-pwd', data)
}
