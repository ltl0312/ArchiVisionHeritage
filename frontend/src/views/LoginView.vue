<template>
  <div class="login-page">
    <div class="login-card">
      <h1 class="login-title">智观·古建</h1>
      <p class="login-subtitle">数字孪生与文化传承</p>

      <el-tabs v-model="activeTab" class="login-tabs">
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" label-position="top">
            <el-form-item label="用户名">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="loginForm.password" type="password" placeholder="请输入密码"
                        @keyup.enter="handleLogin" />
            </el-form-item>
            <el-button type="primary" :loading="loading" block @click="handleLogin">
              登 录
            </el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form :model="regForm" label-position="top">
            <el-form-item label="用户名">
              <el-input v-model="regForm.username" placeholder="2-64个字符" />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="regForm.nickname" placeholder="显示名称" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="regForm.password" type="password" placeholder="至少6位" />
            </el-form-item>
            <el-button type="primary" :loading="loading" block @click="handleRegister">
              注 册
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('login')
const loading = ref(false)

const loginForm = ref({ username: '', password: '' })
const regForm = ref({ username: '', password: '', nickname: '' })

async function handleLogin() {
  if (!loginForm.value.username || !loginForm.value.password) {
    return ElMessage.warning('请填写用户名和密码')
  }
  loading.value = true
  try {
    await userStore.login(loginForm.value)
    ElMessage.success('登录成功')
    router.push('/')
  } catch {
    // 拦截器已处理错误提示
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!regForm.value.username || !regForm.value.password || !regForm.value.nickname) {
    return ElMessage.warning('请填写完整信息')
  }
  loading.value = true
  try {
    await userStore.register(regForm.value)
    ElMessage.success('注册成功，请登录')
    activeTab.value = 'login'
    loginForm.value.username = regForm.value.username
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(180deg, #f5f0e8 0%, #e8e4df 50%, #d5cfc7 100%);
}

.login-card {
  width: 420px;
  padding: 40px;
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-modal);
}

.login-title {
  text-align: center;
  font-size: 28px;
  font-weight: 700;
  color: var(--color-primary);
  letter-spacing: 4px;
  margin-bottom: var(--spacing-xs);
}

.login-subtitle {
  text-align: center;
  color: var(--color-text-muted);
  margin-bottom: var(--spacing-lg);
  font-size: 14px;
  line-height: var(--line-height-body);
}

.login-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
}
</style>
