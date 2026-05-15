<script setup>
import { ref, onMounted } from 'vue'
import { getDeptTree, addDept, updateDept, deleteDept, updateDeptStatus, getDeptUserCount } from '@/api/dept'
import { getDeptRoles, getAvailableRoles, assignRoleToDept, removeRoleFromDept, updateDeptRoleDataScope } from '@/api/role'
import { ElMessage, ElMessageBox } from 'element-plus'

const treeData = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const editingId = ref(null)
const form = ref({ name: '', parentId: null, sort: 1, description: '' })
const parentDisabled = ref(false)

// 角色权限相关
const rolePermDialogVisible = ref(false)
const currentDept = ref(null)  // { id, name }
const deptRoles = ref([])       // 已关联的角色列表
const availableRoles = ref([])  // 可选角色列表

// 删除转移相关
const deleteDialogVisible = ref(false)
const deleteDeptId = ref(null)
const deleteTargetDeptId = ref(null)

// 添加角色表单
const addRoleForm = ref({ roleId: null, dataScope: 'self' })

async function loadTree() {
  const res = await getDeptTree()
  treeData.value = res.data || []
}

function handleAdd(parentId, lockParent) {
  editingId.value = null
  dialogTitle.value = '新增部门'
  parentDisabled.value = !!lockParent
  form.value = { name: '', parentId: lockParent ? parentId : null, sort: 1, description: '' }
  dialogVisible.value = true
}

function handleEdit(row) {
  editingId.value = row.id
  dialogTitle.value = '编辑部门'
  form.value = {
    name: row.name,
    parentId: row.parentId,
    sort: row.sort,
    description: row.description || ''
  }
  dialogVisible.value = true
}

async function handleDelete(id) {
  try {
    const res = await getDeptUserCount(id)
    const userCount = res.data || 0
    if (userCount > 0) {
      deleteDeptId.value = id
      deleteTargetDeptId.value = null
      deleteDialogVisible.value = true
    } else {
      await ElMessageBox.confirm('确认删除该部门？', '提示')
      await deleteDept(id)
      ElMessage.success('删除成功')
      loadTree()
    }
  } catch { /* ignore */ }
}

async function handleDeleteWithTransfer() {
  if (!deleteTargetDeptId.value) {
    ElMessage.warning('请选择目标部门')
    return
  }
  try {
    await deleteDept(deleteDeptId.value, deleteTargetDeptId.value)
    ElMessage.success('删除成功')
    deleteDialogVisible.value = false
    loadTree()
  } catch { /* ignore */ }
}

async function handleStatusChange(id, status) {
  try {
    await updateDeptStatus(id, status ? 1 : 0)
    ElMessage.success(status ? '已启用' : '已禁用')
    loadTree()
  } catch (e) { /* ignore */ }
}

async function handleSave() {
  if (!form.value.name) {
    ElMessage.warning('请输入部门名称')
    return
  }
  if (editingId.value) {
    await updateDept({ id: editingId.value, ...form.value })
    ElMessage.success('修改成功')
  } else {
    await addDept(form.value)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  loadTree()
}

// ===== 角色权限 =====

async function openRolePermDialog(dept) {
  currentDept.value = dept
  rolePermDialogVisible.value = true
  await Promise.all([
    loadDeptRoles(dept.id),
    loadAvailableRoles(dept.id)
  ])
}

async function loadDeptRoles(deptId) {
  try {
    const res = await getDeptRoles(deptId)
    deptRoles.value = res.data || []
  } catch { /* ignore */ }
}

async function loadAvailableRoles(deptId) {
  try {
    const res = await getAvailableRoles(deptId)
    availableRoles.value = res.data || []
  } catch { /* ignore */ }
}

async function handleAssignRole() {
  if (!addRoleForm.value.roleId) {
    ElMessage.warning('请选择要分配的角色')
    return
  }
  try {
    await assignRoleToDept({
      deptId: currentDept.value.id,
      roleId: addRoleForm.value.roleId,
      dataScope: addRoleForm.value.dataScope
    })
    ElMessage.success('角色分配成功')
    addRoleForm.value.roleId = null
    addRoleForm.value.dataScope = 'self'
    await Promise.all([
      loadDeptRoles(currentDept.value.id),
      loadAvailableRoles(currentDept.value.id)
    ])
  } catch { /* ignore */ }
}

async function handleRemoveRole(roleId) {
  try {
    await ElMessageBox.confirm('确认移除该角色？部门下使用此角色的账号将受影响。', '提示')
    await removeRoleFromDept({ deptId: currentDept.value.id, roleId })
    ElMessage.success('角色已移除')
    await Promise.all([
      loadDeptRoles(currentDept.value.id),
      loadAvailableRoles(currentDept.value.id)
    ])
  } catch (e) { /* cancelled */ }
}

async function handleDataScopeChange(roleId, dataScope) {
  try {
    await updateDeptRoleDataScope(currentDept.value.id, roleId, dataScope)
    ElMessage.success('数据范围已更新')
    loadDeptRoles(currentDept.value.id)
  } catch { /* ignore */ }
}


onMounted(loadTree)
</script>

<template>
  <div class="page-container">
    <div class="tool-bar">
      <span style="font-size:18px;font-weight:bold">部门管理</span>
      <el-button type="primary" @click="handleAdd()" v-permission="[1]">新增部门</el-button>
    </div>
    <el-tree
      :data="treeData"
      node-key="id"
      :props="{ children: 'children', label: 'name' }"
      default-expand-all
      :expand-on-click-node="false"
    >
      <template #default="{ node, data }">
        <span class="dept-node">
          <span>{{ data.name }}</span>
          <span class="dept-actions">
            <el-tag v-if="data.status === 0" size="small" type="danger">已禁用</el-tag>
            <el-button link type="primary" size="small" @click.stop="handleAdd(data.id, true)" v-permission="[1]">新增</el-button>
            <el-button link type="primary" size="small" @click.stop="handleEdit(data)" v-permission="[1]">编辑</el-button>
            <el-button
              link
              :type="data.status === 1 ? 'warning' : 'success'"
              size="small"
              @click.stop="handleStatusChange(data.id, data.status === 0)"
              v-permission="[1]"
            >
              {{ data.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="success" size="small" @click.stop="openRolePermDialog(data)" v-permission="[1]">角色权限</el-button>
            <el-button link type="danger" size="small" @click.stop="handleDelete(data.id)" v-permission="[1]">删除</el-button>
          </span>
        </span>
      </template>
    </el-tree>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="部门名称">
          <el-input v-model="form.name" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="form.parentId"
            :data="treeData"
            :props="{ children: 'children', label: 'name', value: 'id' }"
            placeholder="选择上级部门"
            style="width:100%"
            :disabled="parentDisabled"
            check-strictly
          />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="1" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 删除转移对话框 -->
    <el-dialog v-model="deleteDialogVisible" title="部门删除确认" width="500px">
      <p style="margin-bottom:16px">该部门下存在账号，请选择目标部门将账号转移后再删除：</p>
      <el-form label-width="100px">
        <el-form-item label="目标部门">
          <el-tree-select
            v-model="deleteTargetDeptId"
            :data="treeData"
            :props="{ children: 'children', label: 'name', value: 'id' }"
            placeholder="选择目标部门"
            style="width:100%"
            check-strictly
            :disabled="false"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deleteDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleDeleteWithTransfer">转移并删除</el-button>
      </template>
    </el-dialog>

    <!-- 角色权限对话框 -->
    <el-dialog
      v-model="rolePermDialogVisible"
      :title="'角色权限 - ' + (currentDept?.name || '')"
      width="750px"
    >
      <el-tabs type="border-card">
        <el-tab-pane label="分配角色">
          <el-table :data="deptRoles" stripe size="small">
            <el-table-column prop="roleName" label="角色" />
            <el-table-column label="数据范围" width="160">
              <template #default="{ row }">
                <el-select
                  :model-value="row.dataScope"
                  size="small"
                  placeholder="请选择数据范围"
                  @change="handleDataScopeChange(row.roleId, $event)"
                >
                  <el-option label="仅本人" value="self" />
                  <el-option label="本部门" value="dept" />
                  <el-option label="本部门及下级" value="dept_and_sub" />
                  <el-option label="全部" value="all" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column prop="userCount" label="使用人数" width="80" />
            <el-table-column label="操作" width="70">
              <template #default="{ row }">
                <el-button link type="danger" size="small" @click="handleRemoveRole(row.roleId)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div style="margin-top:16px;padding-top:16px;border-top:1px solid #eee">
            <span style="font-size:14px;font-weight:bold">添加角色</span>
            <el-form :model="addRoleForm" inline style="margin-top:8px">
              <el-form-item label="角色">
                <el-select v-model="addRoleForm.roleId" placeholder="请选择" style="width:180px">
                  <el-option v-for="r in availableRoles" :key="r.id" :label="r.name" :value="r.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="数据范围">
                <el-select v-model="addRoleForm.dataScope" placeholder="请选择数据范围" style="width:140px">
                  <el-option label="仅本人" value="self" />
                  <el-option label="本部门" value="dept" />
                  <el-option label="本部门及下级" value="dept_and_sub" />
                  <el-option label="全部" value="all" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleAssignRole">分配</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<style scoped>
.dept-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-right: 16px;
}
.dept-actions {
  display: flex;
  gap: 4px;
  align-items: center;
}
</style>
