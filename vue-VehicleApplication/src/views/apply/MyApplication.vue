<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyApplyList, cancelApply, submitApply } from '@/api/apply'
import { getTemplateList } from '@/api/flow'
import { APPROVAL_STATUS_MAP } from '@/utils/constants'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const query = ref({ status: null, templateId: null, startTime: '', endTime: '' })
const templates = ref([])

async function loadList() {
  const params = { pageNum: pageNum.value, pageSize: pageSize.value }
  Object.keys(query.value).forEach(k => {
    if (query.value[k] !== null && query.value[k] !== '') params[k] = query.value[k]
  })
  const res = await getMyApplyList(params)
  list.value = res.data.records || []
  total.value = res.data.total || 0
}

async function loadTemplates() {
  const res = await getTemplateList()
  templates.value = res.data || []
}

async function handleSubmitDraft(applyId) {
  try {
    await ElMessageBox.confirm('确认提交该草稿？提交后将进入审批流程。', '提示')
    await submitApply(applyId)
    ElMessage.success('提交成功')
    loadList()
  } catch (e) { /* cancelled or error */ }
}

async function handleCancel(applyId) {
  try {
    await ElMessageBox.confirm('确认撤销该申请？', '提示')
    await cancelApply(applyId)
    ElMessage.success('已撤销')
    loadList()
  } catch (e) { /* cancelled */ }
}

function handleSearch() {
  pageNum.value = 1
  loadList()
}

function handleReset() {
  query.value = { status: null, templateId: null, startTime: '', endTime: '' }
  pageNum.value = 1
  loadList()
}

onMounted(() => {
  loadList()
  loadTemplates()
})
</script>

<template>
  <div class="page-container">
    <div class="tool-bar">
      <span style="font-size:18px;font-weight:bold">我的申请</span>
      <el-button type="primary" @click="router.push('/apply/form')">提交申请</el-button>
    </div>

    <el-form :model="query" inline class="search-bar">
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
      <el-table-column label="操作" width="140" fixed="right" @click.stop>
        <template #default="{ row }">
          <el-button
            v-if="row.status === 0"
            link type="success" size="small"
            @click.stop="handleSubmitDraft(row.id)"
          >提交</el-button>
          <el-button
            v-if="row.status === 0 || row.status === 4"
            link type="primary" size="small"
            @click.stop="router.push(`/apply/form/${row.id}`)"
          >修改</el-button>
          <el-button
            v-if="row.status === 1 || row.status === 2"
            link type="danger" size="small"
            @click.stop="handleCancel(row.id)"
          >撤销</el-button>
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
  </div>
</template>
