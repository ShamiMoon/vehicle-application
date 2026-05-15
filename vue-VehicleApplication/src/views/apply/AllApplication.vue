<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getAllApplyList, handleAbnormal, exportApplications } from '@/api/apply'
import { getDeptList } from '@/api/dept'
import { getTemplateList } from '@/api/flow'
import { APPROVAL_STATUS_MAP } from '@/utils/constants'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const query = ref({ applicantName: '', deptId: null, status: null, templateId: null, startTime: '', endTime: '' })
const deptTree = ref([])
const templates = ref([])

const abnormalDialogVisible = ref(false)
const abnormalApplyId = ref(null)
const abnormalForm = ref({ action: 1, reason: '' })

async function loadList() {
  const params = { pageNum: pageNum.value, pageSize: pageSize.value }
  Object.keys(query.value).forEach(k => {
    if (query.value[k] !== null && query.value[k] !== '') params[k] = query.value[k]
  })
  const res = await getAllApplyList(params)
  list.value = res.data.records || []
  total.value = res.data.total || 0
}

async function loadOptions() {
  const [deptRes, templateRes] = await Promise.all([getDeptList(), getTemplateList()])
  deptTree.value = deptRes.data || []
  templates.value = templateRes.data || []
}

function handleSearch() {
  pageNum.value = 1
  loadList()
}

function handleReset() {
  query.value = { applicantName: '', deptId: null, status: null, templateId: null, startTime: '', endTime: '' }
  pageNum.value = 1
  loadList()
}

function openAbnormal(row) {
  abnormalApplyId.value = row.id
  abnormalForm.value = { action: 1, reason: '' }
  abnormalDialogVisible.value = true
}

async function handleExport() {
  try {
    const params = {}
    Object.keys(query.value).forEach(k => {
      if (query.value[k] !== null && query.value[k] !== '') params[k] = query.value[k]
    })
    const res = await exportApplications(params)
    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `用车申请数据_${Date.now()}.xlsx`
    link.click()
    URL.revokeObjectURL(link.href)
    ElMessage.success('导出成功')
  } catch { /* ignore */ }
}

async function handleAbnormalSubmit() {
  if (!abnormalForm.value.reason) {
    ElMessage.warning('请输入处理原因')
    return
  }
  try {
    await handleAbnormal(abnormalApplyId.value, {
      action: abnormalForm.value.action,
      reason: abnormalForm.value.reason
    })
    ElMessage.success('处理成功')
    abnormalDialogVisible.value = false
    loadList()
  } catch (e) { /* ignore */ }
}

onMounted(() => {
  loadList()
  loadOptions()
})
</script>

<template>
  <div class="page-container">
    <div class="tool-bar">
      <span style="font-size:18px;font-weight:bold">全量申请</span>
      <div>
        <el-button @click="handleExport" v-permission="[1,2,3]">导出Excel</el-button>
      </div>
    </div>

    <el-form :model="query" inline class="search-bar">
      <el-form-item label="申请人">
        <el-input v-model="query.applicantName" placeholder="模糊搜索" clearable style="width:140px" />
      </el-form-item>
      <el-form-item label="部门">
        <el-select v-model="query.deptId" placeholder="全部" clearable style="width:140px">
          <el-option v-for="d in deptTree" :key="d.id" :label="d.name" :value="d.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width:120px">
          <el-option v-for="(v, k) in APPROVAL_STATUS_MAP" :key="k" :label="v.label" :value="Number(k)" />
        </el-select>
      </el-form-item>
      <el-form-item label="模板">
        <el-select v-model="query.templateId" placeholder="全部" clearable style="width:160px">
          <el-option v-for="t in templates" :key="t.templateId" :label="t.name" :value="t.templateId" />
        </el-select>
      </el-form-item>
      <el-form-item label="开始日期">
        <el-date-picker v-model="query.startTime" type="date" value-format="YYYY-MM-DD" style="width:140px" />
      </el-form-item>
      <el-form-item label="结束日期">
        <el-date-picker v-model="query.endTime" type="date" value-format="YYYY-MM-DD" style="width:140px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="list" stripe @row-click="row => router.push(`/apply/detail/${row.id}`)" style="cursor:pointer">
      <el-table-column label="标题" min-width="140">
        <template #default="{ row }">
          <span>{{ row.title }}</span>
          <el-tag v-if="row.isUrgent === 1" type="danger" size="small" style="margin-left:4px">紧急</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="applicantName" label="申请人" width="80" />
      <el-table-column prop="deptName" label="部门" width="100" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="APPROVAL_STATUS_MAP[row.status]?.type || 'info'" size="small">
            {{ APPROVAL_STATUS_MAP[row.status]?.label || row.statusName }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="currentNodeName" label="当前节点" width="100" />
      <el-table-column label="提交时间" width="170">
        <template #default="{ row }">{{ $formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="更新时间" width="170">
        <template #default="{ row }">{{ $formatTime(row.updateTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right" @click.stop>
        <template #default="{ row }">
          <el-button
            v-if="row.status === 1 || row.status === 2"
            link type="warning" size="small"
            @click.stop="openAbnormal(row)"
            v-permission="[1,2,3]"
          >异常处理</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top:16px;text-align:right">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @change="loadList"
      />
    </div>

    <el-dialog v-model="abnormalDialogVisible" title="异常处理" width="500px">
      <el-form :model="abnormalForm" label-width="100px">
        <el-form-item label="操作">
          <el-select v-model="abnormalForm.action" placeholder="请选择操作" style="width:100%">
            <el-option label="强制通过" :value="1" />
            <el-option label="强制驳回" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理原因">
          <el-input v-model="abnormalForm.reason" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="abnormalDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAbnormalSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>
