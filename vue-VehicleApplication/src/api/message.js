import request from './request'

export function getMessageList(params) {
  return request.get('/msg/list', { params })
}

export function markMessageRead(messageId) {
  return request.put(`/msg/read/${messageId}`)
}

export function markAllRead() {
  return request.put('/msg/read-all')
}

export function getUnreadCount() {
  return request.get('/msg/unread-count')
}
