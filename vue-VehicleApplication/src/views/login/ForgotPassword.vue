<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { forgotPassword } from '@/api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()

const form = reactive({
  username: '',
  realname: '',
  phone: '',
  email: ''
})
const loading = ref(false)
const submitted = ref(false)

async function handleSubmit() {
  if (!form.username || !form.realname) {
    ElMessage.warning('请填写用户名和真实姓名')
    return
  }
  if (!form.phone && !form.email) {
    ElMessage.warning('手机号和邮箱至少填写一个')
    return
  }
  loading.value = true
  try {
    await forgotPassword(form)
    submitted.value = true
  } catch {
    // error shown by interceptor
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push('/login')
}
</script>

<template>
  <div class="forgot-container">
    <div class="forgot-card">
      <h2 class="forgot-title">找回密码</h2>
      <p class="forgot-subtitle">验证身份后，系统管理员将收到重置密码通知</p>

      <template v-if="!submitted">
        <el-form :model="form" size="large">
          <el-form-item>
            <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="'User'" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.realname" placeholder="请输入真实姓名" :prefix-icon="'Avatar'" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.phone" placeholder="请输入手机号（选填，与邮箱至少填一个）" :prefix-icon="'Iphone'" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.email" placeholder="请输入邮箱（选填，与手机号至少填一个）" :prefix-icon="'Message'" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" style="width:100%" @click="handleSubmit">
              {{ loading ? '提交中...' : '提交申请' }}
            </el-button>
          </el-form-item>
        </el-form>
        <div style="text-align:center;margin-top:12px">
          <el-button link @click="goBack">返回登录</el-button>
        </div>
      </template>

      <template v-else>
        <el-result icon="success" title="申请已提交" sub-title="已通知管理员处理，请留意后续通知">
          <template #extra>
            <el-button type="primary" @click="goBack">返回登录</el-button>
          </template>
        </el-result>
      </template>
    </div>
  </div>
</template>

<style scoped>
.forgot-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.forgot-card {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
}
.forgot-title {
  text-align: center;
  font-size: 24px;
  color: #303133;
  margin-bottom: 4px;
}
.forgot-subtitle {
  text-align: center;
  font-size: 14px;
  color: #909399;
  margin-bottom: 32px;
}
</style>
