<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getApplyDetail, submitApply } from '@/api/apply'
import { agreeApply, rejectApply, transferApply, getApprovalHistory } from '@/api/approve'
import { getUserList } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { APPROVAL_STATUS_MAP, VEHICLE_TYPE_MAP, APPROVAL_ACTION_MAP } from '@/utils/constants'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const detail = ref(null)
const historyList = ref([])
const commentDialogVisible = ref(false)
const transferDialogVisible = ref(false)
const rejectComment = ref('')
const transferForm = ref({ transfereeId: null, comment: '' })
const userOptions = ref([])
const userSearchLoading = ref(false)

async function searchUsers(query) {
  if (!query) { userOptions.value = []; return }
  userSearchLoading.value = true
  try {
    const res = await getUserList({ realname: query, pageNum: 1, pageSize: 20 })
    userOptions.value = (res.data.records || []).map(u => ({ value: u.id, label: `${u.realname}(${u.username})` }))
  } catch { userOptions.value = [] }
  finally { userSearchLoading.value = false }
}

const actionLabelMap = {
  1: '同意',
  2: '驳回',
  3: '转审'
}

const actionTypeMap = {
  1: 'success',
  2: 'danger',
  3: 'warning'
}

async function loadDetail() {
  const applyId = route.params.applyId
  if (!applyId) return
  const res = await getApplyDetail(applyId)
  detail.value = res.data
}

async function loadHistory() {
  const applyId = route.params.applyId
  if (!applyId) return
  try {
    const res = await getApprovalHistory(applyId)
    historyList.value = res.data || []
  } catch {
    historyList.value = []
  }
}

async function handleSubmitDraft() {
  try {
    await ElMessageBox.confirm('确认提交该草稿？提交后将进入审批流程。', '提示')
    await submitApply(detail.value.id)
    ElMessage.success('提交成功')
    loadDetail()
    loadHistory()
  } catch (e) { /* cancelled or error */ }
}

async function handleAgree() {
  try {
    await ElMessageBox.confirm('确认同意该申请？', '提示')
    await agreeApply({ applyId: detail.value.id })
    ElMessage.success('已同意')
    loadDetail()
    loadHistory()
  } catch (e) { /* cancelled */ }
}

function openReject() {
  rejectComment.value = ''
  commentDialogVisible.value = true
}

async function handleReject() {
  if (!rejectComment.value) {
    ElMessage.warning('请输入驳回原因')
    return
  }
  try {
    await rejectApply({ applyId: detail.value.id, comment: rejectComment.value })
    ElMessage.success('已驳回')
    commentDialogVisible.value = false
    loadDetail()
    loadHistory()
  } catch (e) { /* error shown by interceptor */ }
}

function openTransfer() {
  transferForm.value = { transfereeId: null, comment: '' }
  transferDialogVisible.value = true
}

async function handleTransfer() {
  if (!transferForm.value.transfereeId) {
    ElMessage.warning('请选择转审对象')
    return
  }
  try {
    await transferApply({ applyId: detail.value.id, ...transferForm.value })
    ElMessage.success('转审成功')
    transferDialogVisible.value = false
    loadDetail()
    loadHistory()
  } catch (e) { /* error shown by interceptor */ }
}

onMounted(() => {
  loadDetail()
  loadHistory()
})
</script>

<template>
  <div class="page-container" style="max-width:900px;margin:0 auto">
    <div class="tool-bar">
      <span style="font-size:18px;font-weight:bold">申请详情</span>
      <el-button @click="router.back()">返回</el-button>
    </div>

    <template v-if="detail">
      <el-card style="margin-bottom:16px">
        <template #header>基本信息</template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请标题">{{ detail.title }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="APPROVAL_STATUS_MAP[detail.status]?.type || 'info'">
              {{ APPROVAL_STATUS_MAP[detail.status]?.label || detail.statusName }}
            </el-tag>
            <el-tag v-if="detail.isUrgent === 1" type="danger" size="small" style="margin-left:4px">紧急用车</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请人">{{ detail.applicantName }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ detail.deptName }}</el-descriptions-item>
          <el-descriptions-item label="流程模板">{{ detail.templateName }}</el-descriptions-item>
          <el-descriptions-item label="当前节点">{{ detail.currentNodeName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="当前审批人">
            <template v-if="detail.currentApproverNames?.length">
              <el-tag v-for="name in detail.currentApproverNames" :key="name" size="small" style="margin-right:4px">{{ name }}</el-tag>
            </template>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="用车日期">{{ detail.startTime }} ~ {{ detail.endTime }}</el-descriptions-item>
          <el-descriptions-item label="目的地" :span="2">{{ detail.destination }}</el-descriptions-item>
          <el-descriptions-item label="用车事由" :span="2">{{ detail.reason }}</el-descriptions-item>
          <el-descriptions-item label="人数">{{ detail.passengers }}</el-descriptions-item>
          <el-descriptions-item label="车辆类型">{{ VEHICLE_TYPE_MAP[detail.vehicleType] || detail.vehicleTypeName }}</el-descriptions-item>
          <el-descriptions-item label="附件" v-if="detail.attachment" :span="2">
            <el-link :href="detail.attachment" target="_blank" type="primary">查看附件</el-link>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ $formatTime(detail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ $formatTime(detail.updateTime) }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card style="margin-bottom:16px">
        <template #header>审批历史</template>
        <el-timeline v-if="historyList.length > 0">
          <el-timeline-item
            v-for="item in historyList"
            :key="item.id"
            :type="actionTypeMap[item.action] || 'info'"
            :timestamp="item.processTime || '待处理'"
          >
            <strong>{{ item.nodeName }}</strong>
            <div style="margin-top:4px">
              <span>{{ item.approverName || (item.processBy ? '审批人#' + item.processBy : '待审批') }}</span>
              <el-tag v-if="item.action" size="small" :type="actionTypeMap[item.action] || 'info'" style="margin-left:8px">
                {{ APPROVAL_ACTION_MAP[item.action] || actionLabelMap[item.action] }}
              </el-tag>
            </div>
            <div v-if="item.opinion" style="color:#909399;font-size:13px;margin-top:4px">{{ item.opinion }}</div>
            <div v-if="item.transferTo" style="color:#909399;font-size:13px;margin-top:4px">转审目标: {{ item.transferTo }}</div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无审批记录" />
      </el-card>

      <div v-if="detail.status === 0" style="text-align:center;padding:16px">
        <el-button type="primary" size="large" @click="handleSubmitDraft">提交申请</el-button>
        <el-button size="large" @click="router.push(`/apply/form/${detail.id}`)">修改草稿</el-button>
      </div>

      <div v-if="auth.canApprove && (detail.status === 1 || detail.status === 2)" style="text-align:center;padding:16px">
        <el-button type="success" size="large" @click="handleAgree">同意</el-button>
        <el-button type="danger" size="large" @click="openReject">驳回</el-button>
        <el-button size="large" @click="openTransfer">转审</el-button>
      </div>
    </template>

    <el-dialog v-model="commentDialogVisible" title="驳回申请" width="400px">
      <el-input v-model="rejectComment" type="textarea" :rows="4" placeholder="请输入驳回原因（必填）" />
      <template #footer>
        <el-button @click="commentDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="handleReject">确认驳回</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="transferDialogVisible" title="转审" width="400px">
      <el-form :model="transferForm" label-width="80px">
        <el-form-item label="转审对象">
          <el-select
            v-model="transferForm.transfereeId"
            filterable
            remote
            reserve-keyword
            placeholder="搜索姓名"
            :remote-method="searchUsers"
            :loading="userSearchLoading"
            style="width:100%"
          >
            <el-option v-for="u in userOptions" :key="u.value" :label="u.label" :value="u.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="转审原因">
          <el-input v-model="transferForm.comment" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleTransfer">确认转审</el-button>
      </template>
    </el-dialog>
  </div>
</template>
