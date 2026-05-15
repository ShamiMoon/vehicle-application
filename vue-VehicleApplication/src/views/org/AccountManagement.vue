<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getUserList, getUserDetail, addUser, updateUser, deleteUser, resetPassword } from '@/api/user'
import { getDeptList } from '@/api/dept'
import { getRoleList } from '@/api/role'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const users = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const query = ref({ username: '', realname: '', deptId: null, roleId: null, status: null })
const deptTree = ref([])
const roles = ref([])
const dialogVisible = ref(false)
const editingId = ref(null)
const form = ref({
  username: '', password: '', realname: '', phone: '', email: '',
  deptId: null, roleId: null, status: 1
})

async function loadList() {
  const params = { pageNum: pageNum.value, pageSize: pageSize.value }
  Object.keys(query.value).forEach(k => {
    if (query.value[k] !== null && query.value[k] !== '') params[k] = query.value[k]
  })
  const res = await getUserList(params)
  users.value = res.data.records || []
  total.value = res.data.total || 0
}

async function loadOptions() {
  const [deptRes, roleRes] = await Promise.all([getDeptList(), getRoleList()])
  deptTree.value = deptRes.data || []
  roles.value = roleRes.data || []
}

function handleAdd() {
  editingId.value = null
  form.value = { username: '', password: '', realname: '', phone: '', email: '', deptId: null, roleId: null, status: 1 }
  dialogVisible.value = true
}

function handleEdit(row) {
  editingId.value = row.id
  form.value = {
    username: row.username, password: '', realname: row.realname,
    phone: row.phone, email: row.email, deptId: row.deptId, roleId: row.roleId, status: row.status
  }
  dialogVisible.value = true
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确认删除该账号？', '提示')
    await deleteUser(id)
    ElMessage.success('删除成功')
    loadList()
  } catch (e) { /* cancelled */ }
}

async function handleReset(id) {
  try {
    await ElMessageBox.confirm('确认重置该用户密码？', '提示')
    await resetPassword(id)
    ElMessage.success('密码已重置，临时密码已发送至邮箱')
  } catch (e) { /* cancelled */ }
}

async function handleSave() {
  if (!form.value.username || !form.value.realname) {
    ElMessage.warning('请填写必要信息')
    return
  }
  if (editingId.value) {
    const data = { id: editingId.value, ...form.value }
    delete data.password
    if (!data.password) delete data.password
    await updateUser(data)
    ElMessage.success('修改成功')
  } else {
    if (!form.value.password) {
      ElMessage.warning('请输入密码')
      return
    }
    await addUser(form.value)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  loadList()
}

function handleSearch() {
  pageNum.value = 1
  loadList()
}

function handleResetQuery() {
  query.value = { username: '', realname: '', deptId: null, roleId: null, status: null }
  pageNum.value = 1
  loadList()
}

onMounted(async () => {
  const userId = route.query.userId
  if (userId) {
    try {
      const res = await getUserDetail(userId)
      if (res.data && res.data.username) {
        query.value.username = res.data.username
      }
    } catch { /* ignore */ }
  }
  loadList()
  loadOptions()
})
</script>

<template>
  <div class="page-container">
    <div class="tool-bar">
      <span style="font-size:18px;font-weight:bold">账号管理</span>
      <el-button type="primary" @click="handleAdd" v-permission="[1]">新增账号</el-button>
    </div>

    <el-form :model="query" inline class="search-bar">
      <el-form-item label="用户名">
        <el-input v-model="query.username" placeholder="模糊搜索" clearable />
      </el-form-item>
      <el-form-item label="姓名">
        <el-input v-model="query.realname" placeholder="模糊搜索" clearable />
      </el-form-item>
      <el-form-item label="部门">
        <el-select v-model="query.deptId" placeholder="选择部门" clearable style="width:160px">
          <el-option v-for="d in deptTree" :key="d.id" :label="d.name" :value="d.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="角色">
        <el-select v-model="query.roleId" placeholder="选择角色" clearable style="width:160px">
          <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width:100px">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleResetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="users" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="realname" label="真实姓名" />
      <el-table-column prop="phone" label="手机号" width="120" />
      <el-table-column prop="email" label="邮箱" width="180" />
      <el-table-column label="邮件提醒" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.emailNotify === 1 ? 'success' : 'info'" size="small">
            {{ row.emailNotify === 1 ? '开' : '关' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="deptName" label="部门" />
      <el-table-column prop="roleName" label="角色" />
      <el-table-column label="状态" width="70">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最后登录" width="170">
        <template #default="{ row }">{{ $formatTime(row.lastLoginTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleEdit(row)" v-permission="[1]">编辑</el-button>
          <el-button link type="warning" @click="handleReset(row.id)" v-permission="[1]">重置密码</el-button>
          <el-button link type="danger" @click="handleDelete(row.id)" v-permission="[1]">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑账号' : '新增账号'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" :disabled="!!editingId" />
        </el-form-item>
        <el-form-item label="密码" v-if="!editingId">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="form.realname" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="form.deptId" placeholder="选择部门" style="width:100%">
            <el-option v-for="d in deptTree" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleId" placeholder="选择角色" style="width:100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
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
