import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { title: '登录', noAuth: true }
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/login/ForgotPassword.vue'),
    meta: { title: '找回密码', noAuth: true }
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue'),
        meta: { title: '首页', icon: 'Odometer' }
      },
      {
        path: 'dept',
        name: 'DeptManagement',
        component: () => import('@/views/org/DeptManagement.vue'),
        meta: { title: '部门管理', icon: 'OfficeBuilding', roles: [1] }
      },
      {
        path: 'role',
        name: 'RoleManagement',
        component: () => import('@/views/org/RoleManagement.vue'),
        meta: { title: '角色管理', icon: 'UserFilled', roles: [1] }
      },
      {
        path: 'account',
        name: 'AccountManagement',
        component: () => import('@/views/org/AccountManagement.vue'),
        meta: { title: '账号管理', icon: 'Users', roles: [1, 2] }
      },
      {
        path: 'flow-template',
        name: 'TemplateManagement',
        component: () => import('@/views/flow/TemplateManagement.vue'),
        meta: { title: '流程模板', icon: 'List', roles: [1, 2] }
      },
      {
        path: 'flow-approver',
        name: 'ApproverManagement',
        component: () => import('@/views/flow/ApproverManagement.vue'),
        meta: { title: '审批人管理', icon: 'Checked', roles: [1, 2] }
      },
      {
        path: 'apply/my',
        name: 'MyApplication',
        component: () => import('@/views/apply/MyApplication.vue'),
        meta: { title: '我的申请', icon: 'Document' }
      },
      {
        path: 'apply/all',
        name: 'AllApplication',
        component: () => import('@/views/apply/AllApplication.vue'),
        meta: { title: '全量申请', icon: 'Documents', roles: [1, 2] }
      },
      {
        path: 'apply/form',
        name: 'ApplicationForm',
        component: () => import('@/views/apply/ApplicationForm.vue'),
        meta: { title: '提交申请', icon: 'Edit' }
      },
      {
        path: 'apply/form/:applyId',
        name: 'ApplicationFormEdit',
        component: () => import('@/views/apply/ApplicationForm.vue'),
        meta: { title: '修改申请' }
      },
      {
        path: 'apply/detail/:applyId',
        name: 'ApplicationDetail',
        component: () => import('@/views/apply/ApplicationDetail.vue'),
        meta: { title: '申请详情' }
      },
      {
        path: 'approve/pending',
        name: 'PendingList',
        component: () => import('@/views/approve/PendingList.vue'),
        meta: { title: '审批管理', icon: 'Clock', roles: [1, 2, 3, 5] }
      },
      {
        path: 'message',
        name: 'MessageList',
        component: () => import('@/views/message/MessageList.vue'),
        meta: { title: '消息中心', icon: 'Message' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/Profile.vue'),
        meta: { title: '个人设置' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 用车申请审批系统` : '用车申请审批系统'
  if (to.meta.noAuth) {
    next()
    return
  }
  const token = localStorage.getItem('token')
  if (!token) {
    next('/login')
    return
  }
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  const roles = to.meta.roles
  if (roles && roles.length > 0 && !roles.includes(userInfo.roleId)) {
    next('/dashboard')
    return
  }
  next()
})

export default router
