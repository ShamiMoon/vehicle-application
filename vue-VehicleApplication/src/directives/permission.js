import { useAuthStore } from '@/stores/auth'

export default {
  mounted(el, binding) {
    const auth = useAuthStore()
    const roles = binding.value
    if (roles === undefined || roles === null) return
    const allowed = Array.isArray(roles) ? roles : [roles]
    if (!allowed.includes(auth.userInfo.roleId)) {
      el.parentNode?.removeChild(el)
    }
  }
}
