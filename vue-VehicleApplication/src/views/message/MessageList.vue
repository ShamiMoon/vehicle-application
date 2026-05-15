<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMessageList, markMessageRead, markAllRead } from '@/api/message'
import { useAppStore } from '@/stores/app'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const app = useAppStore()
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const filterRead = ref(null)

async function loadList() {
  const params = { pageNum: pageNum.value, pageSize: pageSize.value }
  if (filterRead.value !== null && filterRead.value !== '') params.isRead = filterRead.value
  const res = await getMessageList(params)
  list.value = res.data.records || []
  total.value = res.data.total || 0
}

async function handleMarkRead(messageId) {
  await markMessageRead(messageId)
  loadList()
}

async function handleMarkAllRead() {
  try {
    await ElMessageBox.confirm('确认全部标记为已读？', '提示')
    await markAllRead()
    ElMessage.success('已全部标记为已读')
    loadList()
  } catch (e) { /* cancelled */ }
}

function messageTypeTag(type) {
  const map = { 1: 'warning', 2: 'success', 3: 'danger', 4: 'info', 5: 'danger', 6: 'info', 7: 'warning' }
  return map[type] || 'info'
}

async function goToRelated(message) {
  if (!message.isRead) {
    try {
      await markMessageRead(message.id)
      app.unreadCount = Math.max(0, app.unreadCount - 1)
    } catch { /* ignore */ }
  }
  if (message.messageType === 7) {
    router.push(`/account?userId=${message.applyId}`)
  } else if (message.messageType === 6 && !message.applyId) {
    router.push('/profile?openChangePwd=1')
  } else if (message.applyId) {
    router.push(`/apply/detail/${message.applyId}`)
  }
}

onMounted(() => {
  loadList()
})
</script>

<template>
  <div class="page-container">
    <div class="tool-bar">
      <span style="font-size:18px;font-weight:bold">消息中心</span>
      <el-button v-if="total > 0" @click="handleMarkAllRead">全部标记已读</el-button>
    </div>

    <div class="search-bar">
      <el-select v-model="filterRead" placeholder="全部消息" clearable style="width:140px" @change="loadList">
        <el-option label="未读" :value="0" />
        <el-option label="已读" :value="1" />
      </el-select>
    </div>

    <el-empty v-if="list.length === 0" description="暂无消息" />
    <el-card
      v-for="item in list"
      :key="item.id"
      style="margin-bottom:8px;cursor:pointer"
      shadow="hover"
      @click="goToRelated(item)"
    >
      <div style="display:flex;justify-content:space-between;align-items:center">
        <div style="display:flex;align-items:center;gap:12px">
          <el-tag v-if="!item.isRead" type="danger" size="small">未读</el-tag>
          <el-tag :type="messageTypeTag(item.messageType)" size="small">{{ item.messageTypeName }}</el-tag>
          <div>
            <div style="font-weight:bold">{{ item.title }}</div>
            <div style="color:#909399;font-size:13px;margin-top:4px">{{ item.content }}</div>
          </div>
        </div>
        <div style="display:flex;align-items:center;gap:12px">
          <span style="color:#909399;font-size:12px">{{ $formatTime(item.createTime) }}</span>
          <el-button
            v-if="!item.isRead"
            link type="primary" size="small"
            @click.stop="handleMarkRead(item.id)"
          >标记已读</el-button>
        </div>
      </div>
    </el-card>

    <div style="margin-top:16px;text-align:right" v-if="total > 0">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @change="loadList"
      />
    </div>
  </div>
</template>
