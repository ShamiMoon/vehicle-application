<script setup>
import { ref, onMounted } from 'vue'
import { getTemplateList, addTemplate, updateTemplate, deleteTemplate, updateTemplateStatus, getTemplateDetail } from '@/api/flow'
import { getRoleList } from '@/api/role'
import { getUserList } from '@/api/user'
import { TEMPLATE_TYPE_MAP, APPROVAL_RULE_MAP } from '@/utils/constants'
import { ElMessage, ElMessageBox } from 'element-plus'

const templates = ref([])
const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref(null)
const templateDetail = ref(null)
const form = ref({ name: '', type: null, description: '', nodeConfig: [] })
const allRoles = ref([])
const allUsers = ref([])

async function loadList() {
  const res = await getTemplateList()
  templates.value = res.data || []
}

async function loadOptions() {
  const [roleRes, userRes] = await Promise.all([getRoleList(), getUserList({ pageNum: 1, pageSize: 999 })])
  allRoles.value = roleRes.data || []
  allUsers.value = (userRes.data.records || []).filter(u => [1, 2, 3, 5].includes(u.roleId))
}

function handleAdd() {
  editingId.value = null
  form.value = { name: '', type: null, description: '', nodeConfig: [] }
  addNode()
  dialogVisible.value = true
}

async function handleEdit(row) {
  editingId.value = row.templateId
  const res = await getTemplateDetail(row.templateId)
  const detail = res.data
  form.value = {
    name: detail.name,
    type: detail.type,
    description: detail.description || '',
    nodeConfig: (detail.nodeConfig || []).map(n => ({
      nodeOrder: n.nodeOrder,
      nodeName: n.nodeName,
      approverType: n.approverType,
      approverValue: [...(n.approverValue || [])],
      approveType: n.approveType,
      timeoutHours: n.timeoutHours,
      rejectRule: n.rejectRule || 'return_to_start'
    }))
  }
  dialogVisible.value = true
}

async function handleDelete(templateId) {
  try {
    await ElMessageBox.confirm('确认删除该模板？', '提示')
    await deleteTemplate(templateId)
    ElMessage.success('删除成功')
    loadList()
  } catch (e) { /* cancelled */ }
}

async function handleStatusChange(templateId, status) {
  try {
    await updateTemplateStatus(templateId, status ? 1 : 0)
    ElMessage.success(status ? '已启用' : '已禁用')
    loadList()
  } catch { /* ignore */ }
}

async function showDetail(row) {
  const res = await getTemplateDetail(row.templateId)
  templateDetail.value = res.data
  detailVisible.value = true
}

function addNode() {
  form.value.nodeConfig.push({
    nodeOrder: form.value.nodeConfig.length + 1,
    nodeName: '',
    approverType: 'role',
    approverValue: [],
    approveType: 'single',
    timeoutHours: 24,
    rejectRule: 'return_to_start'
  })
}

function removeNode(index) {
  form.value.nodeConfig.splice(index, 1)
  form.value.nodeConfig.forEach((n, i) => n.nodeOrder = i + 1)
}

async function handleSave() {
  if (!form.value.name) {
    ElMessage.warning('请输入模板名称')
    return
  }
  const data = { ...form.value }
  if (editingId.value) {
    data.templateId = editingId.value
    await updateTemplate(data)
    ElMessage.success('修改成功')
  } else {
    await addTemplate(data)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  loadList()
}

onMounted(() => {
  loadList()
  loadOptions()
})
</script>

<template>
  <div class="page-container">
    <div class="tool-bar">
      <span style="font-size:18px;font-weight:bold">流程模板</span>
      <el-button type="primary" @click="handleAdd" v-permission="[1]">新增模板</el-button>
    </div>
    <el-table :data="templates" stripe>
      <el-table-column prop="templateId" label="ID" width="60" />
      <el-table-column prop="name" label="模板名称" min-width="160" />
      <el-table-column label="流程类型" width="120">
        <template #default="{ row }">{{ TEMPLATE_TYPE_MAP[row.type] || row.typeName }}</template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="160" />
      <el-table-column prop="nodeCount" label="节点数" width="70" />
      <el-table-column prop="createByName" label="创建人" width="100" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="v => handleStatusChange(row.templateId, v)" v-permission="[1]" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ $formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="更新时间" width="170">
        <template #default="{ row }">{{ $formatTime(row.updateTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="showDetail(row)">详情</el-button>
          <el-button link type="primary" @click="handleEdit(row)" v-permission="[1]">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row.templateId)" v-permission="[1]">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑模板' : '新增模板'" width="700px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="模板名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="流程类型">
          <el-select v-model="form.type" placeholder="请选择流程类型" style="width:100%">
            <el-option v-for="(label, key) in TEMPLATE_TYPE_MAP" :key="key" :label="label" :value="Number(key)" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-divider>审批节点配置</el-divider>
        <div v-for="(node, index) in form.nodeConfig" :key="index" style="border:1px solid #dcdfe6;border-radius:6px;padding:16px;margin-bottom:12px">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
            <strong>节点 {{ node.nodeOrder }}</strong>
            <el-button type="danger" link @click="removeNode(index)">删除节点</el-button>
          </div>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="节点名称">
                <el-input v-model="node.nodeName" placeholder="如：部门经理审批" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="审批人类型">
                <el-select v-model="node.approverType" placeholder="请选择审批人类型" style="width:100%">
                  <el-option label="指定角色" value="role" />
                  <el-option label="指定用户" value="user" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="审批人">
                <el-select v-model="node.approverValue" multiple placeholder="请选择角色" style="width:100%" v-if="node.approverType === 'role'">
                  <el-option v-for="r in allRoles" :key="r.id" :label="r.name" :value="r.id" />
                </el-select>
                <el-select v-model="node.approverValue" multiple placeholder="请选择用户" style="width:100%" v-else>
                  <el-option v-for="u in allUsers" :key="u.id" :label="u.realname || u.realName || u.username" :value="u.id" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="审批规则">
                <el-select v-model="node.approveType" placeholder="请选择审批规则" style="width:100%">
                  <el-option v-for="(label, key) in APPROVAL_RULE_MAP" :key="key" :label="label" :value="key" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="超时(小时)">
            <el-input-number v-model="node.timeoutHours" :min="1" :max="720" />
          </el-form-item>
          <el-form-item label="驳回规则">
            <el-select v-model="node.rejectRule" style="width:100%">
              <el-option label="返回发起人（可修改重新提交）" value="return_to_start" />
              <el-option label="直接结束（不可重新提交）" value="end" />
            </el-select>
          </el-form-item>
        </div>
        <el-button type="primary" link @click="addNode">+ 添加节点</el-button>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="模板详情" width="600px">
      <template v-if="templateDetail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="模板名称">{{ templateDetail.name }}</el-descriptions-item>
          <el-descriptions-item label="流程类型">{{ TEMPLATE_TYPE_MAP[templateDetail.type] }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ templateDetail.description }}</el-descriptions-item>
          <el-descriptions-item label="创建人">{{ templateDetail.createByName }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ $formatTime(templateDetail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ $formatTime(templateDetail.updateTime) }}</el-descriptions-item>
        </el-descriptions>
        <el-divider>审批节点</el-divider>
        <el-timeline>
          <el-timeline-item v-for="node in templateDetail.nodeConfig" :key="node.nodeOrder" :timestamp="`${node.approveType === 'all' ? '会签' : '或签'} · ${node.timeoutHours}h超时`">
            <strong>{{ node.nodeName }}</strong>
            <div style="color:#909399;font-size:13px">
              审批人：{{ node.approverValue?.join('、') || '-' }}
            </div>
            <div style="color:#909399;font-size:12px">
              驳回规则：{{ node.rejectRule === 'end' ? '直接结束' : '返回发起人' }}
            </div>
          </el-timeline-item>
        </el-timeline>
      </template>
    </el-dialog>
  </div>
</template>
