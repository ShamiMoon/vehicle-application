<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { saveDraft, submitDirectly, getApplyDetail, updateApply, submitApply } from '@/api/apply'
import { getTemplateList } from '@/api/flow'
import { getDeptList } from '@/api/dept'
import { uploadFile } from '@/api/upload'
import { TEMPLATE_TYPE_MAP, VEHICLE_TYPE_MAP } from '@/utils/constants'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const applyId = computed(() => route.params.applyId)
const isEdit = computed(() => !!applyId.value)

const templates = ref([])
const form = ref({
  title: '',
  templateId: null,
  startTime: '',
  endTime: '',
  reason: '',
  passengers: 1,
  destination: '',
  vehicleType: null,
  attachment: '',
  targetDeptId: null,
  isUrgent: 0
})
const saving = ref(false)
const submitting = ref(false)
const vehicleTypes = ref([
  { type: 1, name: '轿车' },
  { type: 2, name: '商务车' },
  { type: 3, name: '大巴' },
  { type: 4, name: '小巴' },
  { type: 5, name: '其他' }
])

const deptList = ref([])

const selectedTemplate = computed(() => {
  return templates.value.find(t => t.templateId === form.value.templateId)
})

const isSameDay = computed(() => {
  return form.value.startTime && form.value.endTime && form.value.startTime === form.value.endTime
})

watch([() => form.value.startTime, () => form.value.endTime], ([start, end]) => {
  if (start && end && start === end) {
    form.value.isUrgent = 1
  }
})

async function loadTemplates() {
  const res = await getTemplateList()
  templates.value = (res.data || []).filter(t => t.status === 1)
}

async function loadDetail() {
  if (!applyId.value) return
  const res = await getApplyDetail(applyId.value)
  const data = res.data
  form.value = {
    title: data.title,
    templateId: data.templateId,
    startTime: data.startTime,
    endTime: data.endTime,
    reason: data.reason,
    passengers: data.passengers,
    destination: data.destination,
    vehicleType: data.vehicleType,
    attachment: data.attachment || '',
    targetDeptId: data.targetDeptId,
    isUrgent: data.isUrgent ?? 0
  }
}

async function handleUpload(file) {
  try {
    const res = await uploadFile(file)
    form.value.attachment = res.data
    ElMessage.success('上传成功')
  } catch {
    ElMessage.error('上传失败')
  }
}

function validate() {
  if (!form.value.title) { ElMessage.warning('请输入申请标题'); return false }
  if (!form.value.templateId) { ElMessage.warning('请选择流程模板'); return false }
  if (!form.value.startTime || !form.value.endTime) { ElMessage.warning('请选择用车日期'); return false }
  if (form.value.endTime < form.value.startTime) { ElMessage.warning('结束日期不能早于开始日期'); return false }
  if (isSameDay.value && form.value.isUrgent !== 1) { ElMessage.warning('当日用车必须标记为紧急用车'); return false }
  if (!form.value.reason) { ElMessage.warning('请输入用车事由'); return false }
  if (!form.value.destination) { ElMessage.warning('请输入目的地'); return false }
  if (!form.value.vehicleType) { ElMessage.warning('请选择车辆类型'); return false }
  if (selectedTemplate.value?.type === 2 && !form.value.targetDeptId) { ElMessage.warning('跨部门用车请选择目标部门'); return false }
  return true
}

function buildSubmitData() {
  return {
    title: form.value.title,
    templateId: form.value.templateId,
    startTime: form.value.startTime,
    endTime: form.value.endTime,
    reason: form.value.reason,
    passengers: form.value.passengers,
    destination: form.value.destination,
    vehicleType: form.value.vehicleType,
    attachment: form.value.attachment || null,
    targetDeptId: form.value.targetDeptId,
    isUrgent: form.value.isUrgent
  }
}

async function handleSave() {
  if (!validate()) return
  saving.value = true
  try {
    if (isEdit.value) {
      await updateApply({ id: Number(applyId.value), ...buildSubmitData() })
      ElMessage.success('修改成功')
    } else {
      await saveDraft(buildSubmitData())
      ElMessage.success('草稿保存成功')
    }
    router.push('/apply/my')
  } catch (e) {
    // 错误已在 axios 拦截器中提示，此处仅恢复按钮状态
  } finally {
    saving.value = false
  }
}

async function handleSubmit() {
  if (!validate()) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateApply({ id: Number(applyId.value), ...buildSubmitData() })
      await submitApply(Number(applyId.value))
    } else {
      await submitDirectly(buildSubmitData())
    }
    ElMessage.success('提交成功')
    router.push('/apply/my')
  } catch (e) {
    // 错误已在 axios 拦截器中提示
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadTemplates()
  getDeptList().then(res => { deptList.value = res.data || [] }).catch(() => {})
  if (isEdit.value) loadDetail()
})
</script>

<template>
  <div class="page-container" style="max-width:1200px;margin:0 auto">
    <h3 style="margin-bottom:20px">{{ isEdit ? '修改申请' : '提交用车申请' }}</h3>
    <el-form :model="form" label-width="120px">
      <el-form-item label="申请标题">
        <el-input v-model="form.title" placeholder="如：前往客户公司拜访" />
      </el-form-item>
      <el-form-item label="流程模板">
        <el-select v-model="form.templateId" placeholder="选择流程模板" style="width:100%">
          <el-option v-for="t in templates" :key="t.templateId" :label="t.name" :value="t.templateId">
            <span>{{ t.name }}</span>
            <span style="float:right;color:#909399;font-size:12px">{{ TEMPLATE_TYPE_MAP[t.type] }}</span>
          </el-option>
        </el-select>
      </el-form-item>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="开始日期">
            <el-date-picker v-model="form.startTime" type="date" value-format="YYYY-MM-DD" style="width:100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="结束日期">
            <el-date-picker v-model="form.endTime" type="date" value-format="YYYY-MM-DD" style="width:100%" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="用车事由">
        <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请详细说明用车事由" />
      </el-form-item>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="目的地">
            <el-input v-model="form.destination" placeholder="请输入目的地" />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="人数">
            <el-input-number v-model="form.passengers" :min="1" :max="50" controls-position="right" style="width:100%" />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="车辆类型">
            <el-select v-model="form.vehicleType" placeholder="请选择车辆类型" style="width:100%">
              <el-option v-for="v in vehicleTypes" :key="v.type" :label="v.name" :value="v.type" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="紧急用车">
        <el-checkbox v-model="form.isUrgent" :true-value="1" :false-value="0" :disabled="isSameDay">
          <span :style="{ color: isSameDay ? '#e6a23c' : undefined }">{{ isSameDay ? '当日用车必须标记为紧急' : '标记为紧急用车' }}</span>
        </el-checkbox>
        <span style="color:#909399;font-size:12px;margin-left:8px">勾选后允许使用当日或过去的日期</span>
      </el-form-item>

      <el-form-item label="附件">
        <el-upload
          :auto-upload="false"
          :show-file-list="false"
          :on-change="u => handleUpload(u.raw)"
          accept=".jpg,.jpeg,.png,.gif,.pdf,.doc,.docx,.xls,.xlsx"
        >
          <el-button type="primary" link>上传文件</el-button>
          <span style="margin-left:8px;color:#909399;font-size:12px">支持 jpg/png/pdf/doc，不超过 10MB</span>
        </el-upload>
        <div v-if="form.attachment" style="margin-top:8px">
          <el-tag closable @close="form.attachment = ''">{{ form.attachment.split('/').pop() }}</el-tag>
        </div>
      </el-form-item>

      <el-form-item v-if="selectedTemplate?.type === 2" label="目标部门">
        <el-select v-model="form.targetDeptId" placeholder="选择目标部门" style="width:100%" filterable>
          <el-option v-for="d in deptList" :key="d.id" :label="d.name" :value="d.id" />
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">提交申请</el-button>
        <el-button @click="handleSave" :loading="saving">保存草稿</el-button>
        <el-button @click="router.push('/apply/my')">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>
