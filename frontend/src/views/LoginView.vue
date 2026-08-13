<template>
  <div class="login-page animate-fade-in">
    <div class="login-card glass-card">
      <h1 class="login-title">智观·古建</h1>
      <p class="login-subtitle">数字孪生与文化传承</p>

      <el-tabs v-model="activeTab" class="login-tabs" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" label-position="top" @submit.prevent="handleLogin">
            <el-form-item label="用户名或邮箱">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" size="large" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="loginForm.password" type="password" show-password placeholder="请输入密码" size="large" />
            </el-form-item>
            <el-button type="primary" size="large" class="login-submit-btn" :loading="loading" @click="handleLogin">
              登入古建世界
            </el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form :model="registerForm" label-position="top" @submit.prevent="handleRegister">
            <el-form-item label="用户名">
              <el-input v-model="registerForm.username" placeholder="创建用户名" size="large" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="registerForm.email" placeholder="example@mail.com" size="large" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="registerForm.password" type="password" show-password placeholder="至少6位" size="large" />
            </el-form-item>
            <el-button type="primary" size="large" class="login-submit-btn" :loading="loading" @click="handleRegister">
              注册新账号
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
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('login')
const loading = ref(false)
const loginForm = ref({ username: '', password: '' })
const registerForm = ref({ username: '', email: '', password: '' })

async function handleLogin() {
  if (!loginForm.value.username || !loginForm.value.password) {
    return ElMessage.warning('请填写完整信息')
  }
  loading.value = true
  try {
    await userStore.login(loginForm.value)
    ElMessage.success('登录成功')
    router.push('/home')
  } catch {
    ElMessage.error('登录失败，请检查账号密码')
  }
  loading.value = false
}

async function handleRegister() {
  const { username, email, password } = registerForm.value
  if (!username || !email || !password) {
    return ElMessage.warning('请填写完整信息')
  }
  if (password.length < 6) {
    return ElMessage.warning('密码至少6位')
  }
  loading.value = true
  try {
    await userStore.register(registerForm.value)
    ElMessage.success('注册成功，请登录')
    activeTab.value = 'login'
  } catch {
    ElMessage.error('注册失败')
  }
  loading.value = false
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-lg);
}

.login-card {
  width: 420px;
  padding: 40px;
}

.login-title {
  text-align: center;
  font-family: var(--font-family-serif);
  font-size: 28px;
  font-weight: 700;
  color: var(--color-accent);
  letter-spacing: 4px;
  margin-bottom: 4px;
}

.login-subtitle {
  text-align: center;
  font-size: 13px;
  color: var(--color-text-muted);
  letter-spacing: 2px;
  margin-bottom: var(--spacing-lg);
}

.login-tabs {
  margin-bottom: var(--spacing-xs);
}

.login-submit-btn {
  width: 100%;
  margin-top: var(--spacing-sm);
}

@media (max-width: 480px) {
  .login-card {
    width: 100%;
    padding: 24px;
  }
}
</style>
