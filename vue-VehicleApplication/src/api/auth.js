import request from './request'

export function login(data) {
  return request.post('/api/login', data)
}

export function refreshToken() {
  return request.post('/api/refresh')
}

export function validateToken() {
  return request.get('/api/validate')
}

export function forgotPassword(data) {
  return request.post('/api/forgot-password', data)
}
