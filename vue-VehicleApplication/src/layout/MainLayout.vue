<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const app = useAppStore()

const menuItems = computed(() => {
  const roleId = auth.userInfo.roleId
  const items = [
    { path: '/dashboard', title: '首页', icon: 'Odometer' },
    { path: '/apply/form', title: '提交申请', icon: 'Edit' },
    { path: '/apply/my', title: '我的申请', icon: 'Document' }
  ]
  if (roleId === 1) {
    items.push({ path: '/dept', title: '部门管理', icon: 'OfficeBuilding' })
    items.push({ path: '/role', title: '角色管理', icon: 'UserFilled' })
  }
  if (roleId === 1 || roleId === 2) {
    items.push({ path: '/account', title: '账号管理', icon: 'Users' })
    items.push({ path: '/flow-template', title: '流程模板', icon: 'List' })
    items.push({ path: '/flow-approver', title: '审批人管理', icon: 'Checked' })
    items.push({ path: '/apply/all', title: '全量申请', icon: 'Documents' })
  }
  if ([1, 2, 3, 5].includes(roleId)) {
    items.push({ path: '/approve/pending', title: '审批管理', icon: 'Clock' })
  }
  items.push({ path: '/message', title: '消息中心', icon: 'Message' })
  return items
})

function handleLogout() {
  auth.logout()
  router.push('/login')
}

function goHome() {
  router.push('/dashboard')
}
</script>

<template>
  <el-container class="layout-container">
    <el-aside :width="app.sidebarCollapsed ? '64px' : '220px'" class="layout-aside">
      <div class="logo" @click="goHome">
        <el-icon :size="24" color="#409eff"><Odometer /></el-icon>
        <span v-show="!app.sidebarCollapsed">用车审批系统</span>
      </div>
      <el-menu
        :default-active="route.path"
        :collapse="app.sidebarCollapsed"
        background-color="#001529"
        text-color="#ffffffbf"
        active-text-color="#fff"
        router
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <el-button text @click="app.toggleSidebar" style="color:#fff;font-size:20px">
            <el-icon :size="20"><Fold v-if="!app.sidebarCollapsed" /><Expand v-else /></el-icon>
          </el-button>
          <el-breadcrumb separator="/" style="margin-left:16px">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-badge :value="app.unreadCount" :hidden="app.unreadCount === 0" class="msg-badge">
            <el-button text style="color:#fff" @click="router.push('/message')">
              <el-icon :size="20"><Message /></el-icon>
            </el-button>
          </el-badge>
          <el-dropdown trigger="click">
            <span class="user-info">
              <el-avatar :size="32" icon="UserFilled" style="background:#409eff" />
              <span class="username">{{ auth.userInfo.realname || auth.userInfo.realName || auth.userInfo.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/profile')">个人设置</el-dropdown-item>
                <el-dropdown-item @click="goHome">首页</el-dropdown-item>
                <el-dropdown-item @click="handleLogout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout-container {
  height: 100vh;
}
.layout-aside {
  background-color: #001529;
  overflow-y: auto;
  overflow-x: hidden;
  transition: width 0.3s;
}
.layout-aside::-webkit-scrollbar { width: 0 }
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  cursor: pointer;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.el-menu { border-right: none }
.layout-header {
  background: #409eff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  height: 60px;
}
.header-left {
  display: flex;
  align-items: center;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #fff;
  cursor: pointer;
}
.username { font-size: 14px }
.msg-badge :deep(.el-badge__content) { top: 8px; right: 4px; }
.layout-main {
  background: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}
</style>
