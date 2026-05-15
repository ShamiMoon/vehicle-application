<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getMyApplyList } from '@/api/apply'
import { getPendingList } from '@/api/approve'
import { getUnreadCount } from '@/api/message'
import { APPROVAL_STATUS_MAP } from '@/utils/constants'

const router = useRouter()
const auth = useAuthStore()
const stats = ref({
  total: 0,
  approved: 0,
  rejected: 0,
  unread: 0
})
const recentApplies = ref([])
const pendingList = ref([])

onMounted(async () => {
  try {
    const [applyRes, unreadRes] = await Promise.all([
      getMyApplyList({ pageNum: 1, pageSize: 5 }),
      getUnreadCount()
    ])
    const applyData = applyRes.data
    recentApplies.value = applyData.records || []
    stats.value.total = applyData.total || 0
    stats.value.unread = unreadRes.data || 0

    if (auth.canApprove) {
      const pendingRes = await getPendingList()
      pendingList.value = (pendingRes.data || []).slice(0, 5)
    }
  } catch (e) {
    // ignore
  }
})
</script>

<template>
  <div class="dashboard">
    <h3 class="welcome">欢迎回来，{{ auth.userInfo.realname || auth.userInfo.username }}</h3>
    <el-row :gutter="20" style="margin-bottom:20px">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value">{{ stats.total }}</div>
            <div class="stat-label">我的申请</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-value" style="color:#e6a23c">{{ stats.unread }}</div>
            <div class="stat-label">未读消息</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <div style="display:flex;gap:16px;align-items:center;justify-content:center;padding:12px">
            <el-button type="primary" size="large" @click="router.push('/apply/form')">提交用车申请</el-button>
            <el-button size="large" @click="router.push('/apply/my')">查看我的申请</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>最近申请</template>
          <el-table :data="recentApplies" stripe size="small" @row-click="row => router.push(`/apply/detail/${row.id}`)" style="cursor:pointer">
            <el-table-column prop="title" label="标题" min-width="120" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="APPROVAL_STATUS_MAP[row.status]?.type || 'info'" size="small">
                  {{ APPROVAL_STATUS_MAP[row.status]?.label || row.statusName }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12" v-if="auth.canApprove">
        <el-card>
          <template #header>待审批</template>
          <el-table :data="pendingList" stripe size="small" @row-click="row => router.push(`/apply/detail/${row.id}`)" style="cursor:pointer">
            <el-table-column prop="title" label="标题" min-width="120" />
            <el-table-column prop="applicantName" label="申请人" width="80" />
            <el-table-column prop="currentNodeName" label="当前节点" width="100" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.dashboard { max-width: 1200px }
.welcome { font-size: 20px; margin-bottom: 20px; color: #303133 }
.stat-item { text-align: center; padding: 8px }
.stat-value { font-size: 36px; font-weight: bold; color: #409eff }
.stat-label { font-size: 14px; color: #909399; margin-top: 4px }
</style>
