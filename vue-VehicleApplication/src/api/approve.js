import request from './request'

export function getPendingList() {
  return request.get('/apply/sub/pending-list')
}

export function agreeApply(data) {
  return request.post('/apply/app/agree', data)
}

export function rejectApply(data) {
  return request.post('/apply/app/reject', {
    applyId: data.applyId,
    reason: data.comment || data.reason
  })
}

export function transferApply(data) {
  return request.post('/apply/app/transfer', {
    applyId: data.applyId,
    transferTo: String(data.transfereeId || data.transferTo),
    opinion: data.comment || data.opinion || ''
  })
}

export function getApprovalHistory(applyId) {
  return request.get(`/apply/app/history/${applyId}`)
}
