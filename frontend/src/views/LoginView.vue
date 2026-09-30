<template>
  <div class="view-shell login-page">
    <div class="login-card v4-panel v4-panel--float">
      <!-- 品牌 -->
      <div class="brand">
        <span class="brand-seal">
          <el-icon :size="22"><HomeFilled /></el-icon>
        </span>
        <h1 class="brand-name">智观·古建</h1>
        <p class="brand-sub">ARCHIVISION HERITAGE</p>
        <p class="brand-line">古建数字孪生与文化传承平台</p>
      </div>

      <!-- 登录 / 注册 分段控件（V4 语言，替代 el-tabs） -->
      <div class="v4-seg mode-seg" role="tablist" aria-label="登录或注册">
        <button
          role="tab"
          :aria-selected="mode === 'login' ? 'true' : 'false'"
          :class="{ 'is-on': mode === 'login' }"
          @click="switchMode('login')"
        >
          登录
        </button>
        <button
          role="tab"
          :aria-selected="mode === 'register' ? 'true' : 'false'"
          :class="{ 'is-on': mode === 'register' }"
          @click="switchMode('register')"
        >
          注册
        </button>
      </div>

      <!-- 登录 -->
      <el-form
        v-if="mode === 'login'"
        :model="loginForm"
        label-position="top"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="用户名或邮箱">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            size="large"
            autocomplete="username"
          />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="loginForm.password"
            type="password"
            show-password
            placeholder="请输入密码"
            size="large"
            autocomplete="current-password"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <p v-if="errorMsg" class="form-error" role="alert">{{ errorMsg }}</p>

        <button
          type="submit"
          class="v4-btn v4-btn--gold v4-btn--block v4-btn--lg"
          :disabled="loading"
          @click.prevent="handleLogin"
        >
          <el-icon v-if="loading" :size="16" class="loading-spin"><Loading /></el-icon>
          {{ loading ? '正在登入…' : '登入古建世界' }}
        </button>
      </el-form>

      <!-- 注册 -->
      <el-form
        v-else
        :model="registerForm"
        label-position="top"
        @submit.prevent="handleRegister"
      >
        <el-form-item label="用户名">
          <el-input
            v-model="registerForm.username"
            placeholder="创建用户名"
            size="large"
            autocomplete="username"
          />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input
            v-model="registerForm.email"
            placeholder="example@mail.com"
            size="large"
            autocomplete="email"
          />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="registerForm.password"
            type="password"
            show-password
            placeholder="至少 6 位"
            size="large"
            autocomplete="new-password"
            @keyup.enter="handleRegister"
          />
        </el-form-item>

        <p v-if="errorMsg" class="form-error" role="alert">{{ errorMsg }}</p>

        <button
          type="submit"
          class="v4-btn v4-btn--gold v4-btn--block v4-btn--lg"
          :disabled="loading"
          @click.prevent="handleRegister"
        >
          <el-icon v-if="loading" :size="16" class="loading-spin"><Loading /></el-icon>
          {{ loading ? '正在提交…' : '注册新账号' }}
        </button>
      </el-form>

      <p class="login-note v4-note">
        注册后即可使用古建智析与一键幻筑。社区内容经审核后公开。
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { HomeFilled, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const mode = ref('login')
const loading = ref(false)
/** 页内错误：不依赖一闪而过的 toast，让失败原因留在界面上 */
const errorMsg = ref('')

const loginForm = ref({ username: '', password: '' })
const registerForm = ref({ username: '', email: '', password: '' })

/* 切换模式时清掉上一次的错误，避免串台 */
watch(mode, () => { errorMsg.value = '' })

function switchMode(next) {
  if (loading.value) return
  mode.value = next
}

async function handleLogin() {
  errorMsg.value = ''
  if (!loginForm.value.username || !loginForm.value.password) {
    errorMsg.value = '请填写完整的用户名与密码'
    return
  }
  loading.value = true
  try {
    await userStore.login(loginForm.value)
    ElMessage.success('登录成功')
    router.push('/home')
  } catch (e) {
    // 拦截器已弹提示；这里再在页内保留一份，便于用户核对
    errorMsg.value = e?.message || '登录失败，请检查用户名与密码'
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  errorMsg.value = ''
  const { username, email, password } = registerForm.value
  if (!username || !email || !password) {
    errorMsg.value = '请填写完整的用户名、邮箱与密码'
    return
  }
  if (password.length < 6) {
    errorMsg.value = '密码至少 6 位'
    return
  }
  loading.value = true
  try {
    await userStore.register(registerForm.value)
    ElMessage.success('注册成功，请登录')
    mode.value = 'login'
    loginForm.value.username = username
    loginForm.value.password = ''
  } catch (e) {
    errorMsg.value = e?.message || '注册失败，请更换用户名或邮箱后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-lg);
}

.login-card {
  width: 420px;
  max-width: 100%;
  padding: 40px;
}

/* ═══ 品牌 ═══ */
.brand {
  text-align: center;
  margin-bottom: var(--spacing-lg);
}

.brand-seal {
  width: 48px;
  height: 48px;
  margin: 0 auto 14px;
  border-radius: var(--radius-lg);
  display: grid;
  place-items: center;
  background: var(--color-accent-soft);
  border: 1px solid var(--color-border-gold);
  color: var(--color-accent-text);
}

.brand-name {
  font-family: var(--font-family-serif);
  font-size: 26px;
  font-weight: 600;
  color: var(--color-text-main);
  letter-spacing: 4px;
}

.brand-sub {
  font-size: 9px;
  color: var(--color-text-ghost);
  letter-spacing: 0.16em;
  text-transform: uppercase;
  margin-top: 2px;
}

.brand-line {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 10px;
}

/* ═══ 模式切换 ═══ */
.mode-seg {
  justify-content: center;
  gap: 8px;
  margin-bottom: var(--spacing-lg);
}

.mode-seg > button {
  flex: 1;
  height: 34px;
  font-size: 13px;
}

/* ═══ 页内错误 ═══ */
.form-error {
  margin-bottom: var(--spacing-sm);
  padding: 9px 12px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  line-height: 1.7;
  color: var(--color-rose-text);
  background: var(--color-rose-soft);
  border: 1px solid var(--color-border-rose);
}

.login-note {
  text-align: center;
  border-top: none;
  padding-top: var(--spacing-md);
  margin-top: var(--spacing-md);
  border-top: 1px solid var(--color-border-light);
}
</style>
