import request from './request'

export function addTemplate(data) {
  return request.post('/flow/template/add', data)
}

export function updateTemplate(data) {
  return request.put('/flow/template/update', data)
}

export function deleteTemplate(templateId) {
  return request.delete(`/flow/template/delete/${templateId}`)
}

export function getTemplateList(params) {
  return request.get('/flow/template/list', { params })
}

export function getTemplateDetail(templateId) {
  return request.get(`/flow/template/detail/${templateId}`)
}

export function updateTemplateStatus(templateId, status) {
  return request.put(`/flow/template/status/${templateId}/${status}`)
}
