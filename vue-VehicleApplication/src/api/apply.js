import request from './request'

export function saveDraft(data) {
  return request.post('/apply/sub/save', data)
}

export function submitApply(applyId) {
  return request.put(`/apply/sub/submit/${applyId}`)
}

export function submitDirectly(data) {
  return request.post('/apply/sub/submit-directly', data)
}

export function updateApply(data) {
  return request.put('/apply/sub/update', data)
}

export function cancelApply(applyId) {
  return request.delete(`/apply/sub/cancel/${applyId}`)
}

export function getMyApplyList(params) {
  return request.get('/apply/sub/my-list', { params })
}

export function getAllApplyList(params) {
  return request.get('/apply/sub/all-list', { params })
}

export function getApplyDetail(applyId) {
  return request.get(`/apply/sub/detail/${applyId}`)
}

export function getVehicleTypes(templateType) {
  return request.get(`/apply/sub/vehicle-types/${templateType}`)
}

export function getApprovedByMe(params) {
  return request.get('/apply/sub/approved-by-me', { params })
}

export function handleAbnormal(applyId, data) {
  return request.post(`/apply/sub/handle-abnormal/${applyId}`, null, { params: data })
}

export async function exportApplications(params) {
  const res = await request.get('/export', { params, responseType: 'blob' })
  return res  // res 已经是完整响应（含 blob 数据）
}
