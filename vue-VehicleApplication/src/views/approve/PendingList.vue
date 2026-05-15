<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getPendingList, agreeApply, rejectApply, transferApply } from '@/api/approve'
import { getApprovedByMe } from '@/api/apply'
import { getUserList } from '@/api/user'
import { APPROVAL_STATUS_MAP } from '@/utils/constants'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const activeTab = ref('pending')
const pendingList = ref([])
const approvedList = ref([])
const approvedTotal = ref(0)
const approvedPageNum = ref(1)
const approvedPageSize = ref(10)

const rejectDialogVisible = ref(false)
const rejectApplyId = ref(null)
const rejectComment = ref('')

const transferDialogVisible = ref(false)
const transferApplyId = ref(null)
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

async function loadPendingList() {
  const res = await getPendingList()
  pendingList.value = res.data || []
}

async function loadApprovedList() {
  const params = { pageNum: approvedPageNum.value, pageSize: approvedPageSize.value }
  const res = await getApprovedByMe(params)
  approvedList.value = res.data.records || []
  approvedTotal.value = res.data.total || 0
}

function loadList() {
  if (activeTab.value === 'pending') {
    loadPendingList()
  } else {
    loadApprovedList()
  }
}

async function handleAgree(applyId) {
  try {
    await ElMessageBox.confirm('确认同意该申请？', '提示')
    await agreeApply({ applyId })
    ElMessage.success('已同意')
    loadPendingList()
  } catch (e) {
    // ElMessageBox 取消或 API 错误（已在拦截器提示）
  }
}

function openReject(applyId) {
  rejectApplyId.value = applyId
  rejectComment.value = ''
  rejectDialogVisible.value = true
}

async function handleReject() {
  if (!rejectComment.value) {
    ElMessage.warning('请输入驳回原因')
    return
  }
  try {
    await rejectApply({ applyId: rejectApplyId.value, comment: rejectComment.value })
    ElMessage.success('已驳回')
    rejectDialogVisible.value = false
    loadPendingList()
  } catch (e) { /* error shown by interceptor */ }
}

function openTransfer(applyId) {
  transferApplyId.value = applyId
  transferForm.value = { transfereeId: null, comment: '' }
  transferDialogVisible.value = true
}

async function handleTransfer() {
  if (!transferForm.value.transfereeId) {
    ElMessage.warning('请选择转审对象')
    return
  }
  try {
    await transferApply({ applyId: transferApplyId.value, ...transferForm.value })
    ElMessage.success('转审成功')
    transferDialogVisible.value = false
    loadPendingList()
  } catch (e) { /* error shown by interceptor */ }
}

onMounted(loadPendingList)
</script>

<template>
  <div class="page-container">
    <div class="tool-bar">
      <span style="font-size:18px;font-weight:bold">审批管理</span>
    </div>

    <el-tabs v-model="activeTab" @tab-change="loadList">
      <el-tab-pane label="待我审批" name="pending">
        <el-table :data="pendingList" stripe v-if="pendingList.length > 0">
          <el-table-column label="标题" min-width="140">
            <template #default="{ row }">
              <span>{{ row.title }}</span>
              <el-tag v-if="row.isUrgent === 1" type="danger" size="small" style="margin-left:4px">紧急</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="applicantName" label="申请人" width="80" />
          <el-table-column prop="deptName" label="部门" width="100" />
          <el-table-column prop="currentNodeName" label="当前节点" width="100" />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="APPROVAL_STATUS_MAP[row.status]?.type || 'info'" size="small">
                {{ APPROVAL_STATUS_MAP[row.status]?.label || row.statusName }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" width="170">
            <template #default="{ row }">{{ $formatTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="更新时间" width="170">
            <template #default="{ row }">{{ $formatTime(row.updateTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="router.push(`/apply/detail/${row.id}`)">详情</el-button>
              <el-button link type="success" size="small" @click="handleAgree(row.id)">同意</el-button>
              <el-button link type="danger" size="small" @click="openReject(row.id)">驳回</el-button>
              <el-button link type="warning" size="small" @click="openTransfer(row.id)">转审</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无待审批申请" />
      </el-tab-pane>

      <el-tab-pane label="我已审批" name="approved">
        <el-table :data="approvedList" stripe v-if="approvedList.length > 0">
          <el-table-column label="标题" min-width="140">
            <template #default="{ row }">
              <span>{{ row.title }}</span>
              <el-tag v-if="row.isUrgent === 1" type="danger" size="small" style="margin-left:4px">紧急</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="applicantName" label="申请人" width="80" />
          <el-table-column prop="deptName" label="部门" width="100" />
          <el-table-column prop="currentNodeName" label="当前节点" width="100" />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="APPROVAL_STATUS_MAP[row.status]?.type || 'info'" size="small">
                {{ APPROVAL_STATUS_MAP[row.status]?.label || row.statusName }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" width="170">
            <template #default="{ row }">{{ $formatTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="更新时间" width="170">
            <template #default="{ row }">{{ $formatTime(row.updateTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="router.push(`/apply/detail/${row.id}`)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无已审批申请" />
        <div style="margin-top:16px;text-align:right" v-if="approvedTotal > 0">
          <el-pagination
            v-model:current-page="approvedPageNum"
            v-model:page-size="approvedPageSize"
            :total="approvedTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @change="loadApprovedList"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="rejectDialogVisible" title="驳回申请" width="400px">
      <el-input v-model="rejectComment" type="textarea" :rows="4" placeholder="请输入驳回原因（必填）" />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
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
