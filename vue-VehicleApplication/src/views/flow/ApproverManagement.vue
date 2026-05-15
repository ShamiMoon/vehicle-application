<script setup>
import { ref, onMounted, computed } from 'vue'
import { getTemplateList } from '@/api/flow'
import { getApproverList, assignApprover, deleteApprover } from '@/api/approver'
import { getRoleList } from '@/api/role'
import { getUserList } from '@/api/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const templates = ref([])
const selectedTemplateId = ref(null)
const approverTree = ref([])
const allRoles = ref([])
const allUsers = ref([])
const editDialogVisible = ref(false)
const editingNode = ref(null)

const userMap = computed(() => {
  const map = {}
  allUsers.value.forEach(u => { map[u.id] = u })
  return map
})

const roleMap = computed(() => {
  const map = {}
  allRoles.value.forEach(r => { map[r.id] = r })
  return map
})

async function loadTemplates() {
  const res = await getTemplateList()
  templates.value = res.data || []
}

async function loadApprovers() {
  if (!selectedTemplateId.value) {
    approverTree.value = []
    return
  }
  const res = await getApproverList(selectedTemplateId.value)
  approverTree.value = (res.data || []).map(node => ({
    ...node,
    userDetails: (node.userIds || []).map(id => userMap.value[id] || { id, realname: `用户${id}`, username: `user${id}` })
  }))
}

async function loadOptions() {
  const [roleRes, userRes] = await Promise.all([getRoleList(), getUserList({ pageNum: 1, pageSize: 999 })])
  allRoles.value = roleRes.data || []
  allUsers.value = userRes.data.records || []
}

function handleEdit(node) {
  editingNode.value = {
    ...node,
    approverIds: [...(node.userIds || [])]
  }
  editDialogVisible.value = true
}

async function handleSaveApprover() {
  const data = {
    templateId: selectedTemplateId.value,
    nodeOrder: editingNode.value.nodeOrder,
    approverIds: editingNode.value.approverIds
  }
  try {
    await assignApprover(data)
    ElMessage.success('分配成功')
    editDialogVisible.value = false
    loadApprovers()
  } catch (e) { /* ignore */ }
}

async function handleDeleteApprover(nodeOrder, userId) {
  try {
    await ElMessageBox.confirm('确认删除该审批人？', '提示')
    await deleteApprover({
      templateId: selectedTemplateId.value,
      nodeOrder: nodeOrder,
      userId: userId
    })
    ElMessage.success('删除成功')
    loadApprovers()
  } catch (e) { /* cancelled */ }
}

onMounted(() => {
  loadTemplates()
  loadOptions()
})
</script>

<template>
  <div class="page-container">
    <div class="tool-bar">
      <span style="font-size:18px;font-weight:bold">审批人管理</span>
    </div>
    <div class="search-bar">
      <el-select v-model="selectedTemplateId" placeholder="请选择流程模板" style="width:300px" @change="loadApprovers">
        <el-option v-for="t in templates" :key="t.templateId" :label="t.name" :value="t.templateId" />
      </el-select>
    </div>

    <template v-if="selectedTemplateId">
      <el-card v-for="node in approverTree" :key="node.nodeOrder" style="margin-bottom:12px">
        <template #header>
          <div style="display:flex;justify-content:space-between;align-items:center">
            <span>
              <strong>节点 {{ node.nodeOrder }}：{{ node.nodeName }}</strong>
              <el-tag size="small" type="info" style="margin-left:8px">{{ node.approverType }}</el-tag>
            </span>
            <el-button type="primary" size="small" @click="handleEdit(node)">分配审批人</el-button>
          </div>
        </template>
        <el-table :data="node.userDetails || []" size="small">
          <el-table-column prop="id" label="用户ID" width="80" />
          <el-table-column prop="realname" label="姓名" />
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button link type="danger" size="small" @click="handleDeleteApprover(node.nodeOrder, row.id)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="!node.userDetails?.length" style="text-align:center;padding:20px;color:#909399">暂未分配审批人</div>
      </el-card>
    </template>

    <el-dialog v-model="editDialogVisible" title="分配审批人" width="500px">
      <el-form v-if="editingNode" label-width="100px">
        <el-form-item label="节点名称">{{ editingNode.nodeName }}</el-form-item>
        <el-form-item label="审批人">
          <el-select v-model="editingNode.approverIds" multiple filterable placeholder="请选择审批人" style="width:100%">
            <el-option v-for="u in allUsers" :key="u.id" :label="u.realname || u.realName || u.username" :value="u.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveApprover">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
