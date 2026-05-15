import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref((localStorage.getItem('token') || '').replace(/^Bearer\s+/i, ''))
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'))

  const isLoggedIn = computed(() => !!token.value)
  const roleId = computed(() => userInfo.value.roleId || 0)
  const roleName = computed(() => userInfo.value.roleName || '')
  const isAdmin = computed(() => roleId.value === 1)
  const isCarAdmin = computed(() => roleId.value === 1 || roleId.value === 3)
  const isApprover = computed(() => roleId.value === 5)
  const canApprove = computed(() => [1, 2, 3, 5].includes(roleId.value))

  function sanitizeToken(raw) {
    return raw ? raw.replace(/^Bearer\s+/i, '') : ''
  }

  async function login(credentials) {
    const res = await loginApi(credentials)
    const data = res.data
    token.value = sanitizeToken(data.token)
    userInfo.value = data
    localStorage.setItem('token', token.value)
    localStorage.setItem('userInfo', JSON.stringify(data))
    return data
  }

  function logout() {
    token.value = ''
    userInfo.value = {}
    localStorage.clear()
  }

  return { token, userInfo, isLoggedIn, roleId, roleName, isAdmin, isCarAdmin, isApprover, canApprove, login, logout }
})
