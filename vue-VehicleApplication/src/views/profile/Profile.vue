<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { updateProfile, changePassword, getUserDetail } from '@/api/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const loading = ref(false)
const detail = ref(null)
const form = ref({ realname: '', phone: '', email: '', emailNotify: 0 })
const pwdDialogVisible = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })

async function loadProfile() {
  try {
    const res = await getUserDetail(auth.userInfo.userId)
    detail.value = res.data
    const data = res.data
    form.value = {
      realname: data.realname || '',
      phone: data.phone || '',
      email: data.email || '',
      emailNotify: data.emailNotify ?? 0
    }
  } catch { /* ignore */ }
}

async function handleSave() {
  if (!form.value.realname) {
    ElMessage.warning('姓名不能为空')
    return
  }
  loading.value = true
  try {
    await updateProfile(form.value)
    ElMessage.success('保存成功')
  } catch { /* error shown by interceptor */ }
  finally { loading.value = false }
}

function openChangePwd() {
  pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  pwdDialogVisible.value = true
}

async function handleChangePwd() {
  if (!pwdForm.value.oldPassword || !pwdForm.value.newPassword) {
    ElMessage.warning('请填写完整')
    return
  }
  if (pwdForm.value.newPassword !== pwdForm.value.confirmPassword) {
    ElMessage.warning('两次密码输入不一致')
    return
  }
  if (pwdForm.value.newPassword.length < 6) {
    ElMessage.warning('密码长度不能少于6位')
    return
  }
  try {
    await changePassword({
      id: auth.userInfo.userId,
      oldPassword: pwdForm.value.oldPassword,
      newPassword: pwdForm.value.newPassword
    })
    ElMessage.success('密码修改成功')
    pwdDialogVisible.value = false
  } catch { /* error shown by interceptor */ }
}

async function handleDisable() {
  try {
    await ElMessageBox.confirm(
      '确认注销当前账号？注销后您将无法登录系统。',
      '危险操作',
      { confirmButtonText: '确认注销', cancelButtonText: '取消', type: 'warning' }
    )
    await auth.logout()
    router.push('/login')
  } catch { /* cancelled */ }
}

onMounted(() => {
  loadProfile()
  if (route.query.openChangePwd === '1') {
    openChangePwd()
  }
})
</script>

<template>
  <div class="page-container" style="max-width:600px;margin:0 auto">
    <h3 style="margin-bottom:20px">个人设置</h3>

    <el-card style="margin-bottom:16px">
      <template #header>基本信息</template>
      <el-form :model="form" label-width="100px">
        <el-form-item label="用户名">
          <el-input :model-value="auth.userInfo.username" disabled />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.realname" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="部门">
          <el-input :model-value="auth.userInfo.deptName || detail?.deptName || '-'" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-input :model-value="auth.userInfo.roleName || detail?.roleName || '-'" disabled />
        </el-form-item>
        <el-form-item label="权限范围">
          <el-input :model-value="detail?.dataScope || '-'" disabled />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="loading">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-bottom:16px">
      <template #header>通知设置</template>
      <el-form label-width="100px">
        <el-form-item label="邮件提醒">
          <el-switch
            v-model="form.emailNotify"
            :active-value="1"
            :inactive-value="0"
            @change="handleSave"
          />
          <span style="color:#909399;font-size:12px;margin-left:8px">开启后审批相关通知将发送到绑定邮箱</span>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-bottom:16px">
      <template #header>安全设置</template>
      <el-form label-width="100px">
        <el-form-item label="登录密码">
          <el-button @click="openChangePwd">修改密码</el-button>
        </el-form-item>
        <el-form-item label="注销账号">
          <el-button type="danger" @click="handleDisable">注销当前账号</el-button>
          <span style="color:#909399;font-size:12px;margin-left:8px">注销后无法恢复</span>
        </el-form-item>
      </el-form>
    </el-card>

    <el-dialog v-model="pwdDialogVisible" title="修改密码" width="400px">
      <el-form :model="pwdForm" label-width="100px">
        <el-form-item label="原密码">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleChangePwd">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>
