<script setup>
import { ref, onMounted } from 'vue'
import { getRoleList, addRole, updateRole, deleteRole, updateRoleStatus } from '@/api/role'
import { ElMessage, ElMessageBox } from 'element-plus'

const roles = ref([])
const dialogVisible = ref(false)
const editingId = ref(null)
const form = ref({ name: '', description: '', status: 1 })

async function loadList() {
  const res = await getRoleList()
  roles.value = res.data || []
}

function handleAdd() {
  editingId.value = null
  form.value = { name: '', description: '', status: 1 }
  dialogVisible.value = true
}

function handleEdit(row) {
  editingId.value = row.id
  form.value = {
    name: row.name,
    description: row.description || '',
    status: row.status
  }
  dialogVisible.value = true
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确认删除该角色？', '提示')
    await deleteRole(id)
    ElMessage.success('删除成功')
    loadList()
  } catch (e) { /* cancelled */ }
}

async function handleStatusChange(row) {
  try {
    await updateRoleStatus(row.id, row.status)
    ElMessage.success(row.status === 1 ? '已启用' : '已禁用')
  } catch {
    loadList()
  }
}

async function handleSave() {
  if (!form.value.name) {
    ElMessage.warning('请输入角色名称')
    return
  }
  if (editingId.value) {
    await updateRole({ id: editingId.value, ...form.value })
    ElMessage.success('修改成功')
  } else {
    await addRole(form.value)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  loadList()
}

onMounted(loadList)
</script>

<template>
  <div class="page-container">
    <div class="tool-bar">
      <span style="font-size:18px;font-weight:bold">角色管理</span>
      <el-button type="primary" @click="handleAdd">新增角色</el-button>
    </div>
    <el-table :data="roles" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="角色名称" />
      <el-table-column prop="description" label="描述" />
      <el-table-column prop="userCount" label="关联用户数" width="100" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="handleStatusChange(row)" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="180">
        <template #default="{ row }">{{ row.createTime }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑角色' : '新增角色'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="角色名称">
          <el-input v-model="form.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
