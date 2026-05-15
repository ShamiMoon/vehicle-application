import request from './request'

export function assignApprover(data) {
  return request.post('/flow/approver/assign', {
    templateId: data.templateId,
    nodeOrder: data.nodeOrder,
    userIds: data.approverIds || data.userIds
  })
}

export function batchAssignApprover(data) {
  return request.post('/flow/approver/batch-assign', data)
}

export function updateApprover(data) {
  return request.put('/flow/approver/update', {
    templateId: data.templateId,
    nodeOrder: data.nodeOrder,
    approverType: data.approverType,
    approverValue: data.approverValue || data.approverIds
  })
}

export function deleteApprover(params) {
  return request.delete('/flow/approver/delete', { params })
}

export function getApproverList(templateId) {
  return request.get(`/flow/approver/list/${templateId}`)
}
