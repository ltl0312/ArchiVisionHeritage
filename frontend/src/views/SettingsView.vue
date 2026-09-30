<template>
  <div class="view-shell">
    <!-- ═══ 工具行：返回 + 账号摘要 ═══ -->
    <div class="v4-toolrow">
      <button type="button" class="v4-btn v4-btn--ghost v4-btn--sm" @click="goBack">
        <el-icon :size="14"><ArrowLeft /></el-icon>返回
      </button>

      <div class="v4-headstats">
        <template v-if="profile?.username">
          <span class="v4-pill v4-pill--mut">{{ profile.username }}</span>
          <span class="v4-pill" :class="isAdmin ? 'v4-pill--gold' : 'v4-pill--jade'">
            {{ profile.role }}
          </span>
        </template>
      </div>
    </div>

    <div class="v4-settings-grid">
      <!-- ═══════════ ① 视觉偏好 · VISUAL ═══════════ -->
      <section class="v4-card">
        <div class="v4-cardhd">
          <h3>视觉偏好</h3>
          <span class="k">VISUAL</span>
        </div>

        <!-- 昼夜流转：读 / 写同一个 themeStore，与侧栏菜单双向同步 -->
        <div class="v4-kv-row">
          <span>昼夜流转</span>
          <div class="v4-seg v4-seg--sm" role="group" aria-label="昼夜流转">
            <button
              type="button"
              :class="{ 'is-on': themeStore.isDark }"
              :aria-pressed="themeStore.isDark"
              @click="themeStore.setTheme('dark')"
            >
              玄墨
            </button>
            <button
              type="button"
              :class="{ 'is-on': !themeStore.isDark }"
              :aria-pressed="!themeStore.isDark"
              @click="themeStore.setTheme('light')"
            >
              宣纸
            </button>
          </div>
        </div>

        <!-- 信息密度：切换 html.density-compact，真实改变 .view-shell / .v4-card 等间距 -->
        <div class="v4-kv-row">
          <span>信息密度</span>
          <div class="v4-seg v4-seg--sm" role="group" aria-label="信息密度">
            <button
              type="button"
              :class="{ 'is-on': themeStore.density === 'cozy' }"
              :aria-pressed="themeStore.density === 'cozy'"
              @click="themeStore.applyDensity('cozy')"
            >
              舒展
            </button>
            <button
              type="button"
              :class="{ 'is-on': themeStore.density === 'compact' }"
              :aria-pressed="themeStore.density === 'compact'"
              @click="themeStore.applyDensity('compact')"
            >
              紧凑
            </button>
          </div>
        </div>

        <p class="v4-note">
          玄墨为默认模式，为路演投影与三维素材提供最高对比度；宣纸适合长时间阅读文献。
        </p>
      </section>

      <!-- ═══════════ ② 账号资料 · ACCOUNT ═══════════ -->
      <section class="v4-card">
        <div class="v4-cardhd">
          <h3>账号资料</h3>
          <span class="k">ACCOUNT</span>
        </div>

        <!-- 加载中 -->
        <div v-if="profileLoading" class="sk-lines" aria-busy="true" aria-live="polite">
          <span v-for="i in 4" :key="i" class="sk-line" :class="i % 2 ? 'sk-line--60' : 'sk-line--85'"></span>
        </div>

        <!-- 错误：说明 + 重试 -->
        <div v-else-if="profileError" class="v4-empty card-state" role="alert">
          <svg viewBox="0 0 40 40" aria-hidden="true">
            <path d="M20 8L34 32H6L20 8Z" />
            <path d="M20 17.5V24.5" />
            <path d="M20 28.4H20.02" />
          </svg>
          <p>{{ profileError }}</p>
          <button type="button" class="v4-btn v4-btn--outline v4-btn--sm" @click="loadProfile">
            重试
          </button>
        </div>

        <!-- 只读态 -->
        <template v-else-if="!editing">
          <div class="v4-kv">
            <span class="k">昵称</span>
            <span class="v">{{ profile.nickname || '未设置' }}</span>
          </div>
          <div class="v4-kv">
            <span class="k">用户名</span>
            <span class="v v--mut">{{ profile.username || '—' }}</span>
          </div>
          <div class="v4-kv">
            <span class="k">角色</span>
            <span class="v" :class="isAdmin ? 'v--gold' : 'v--jade'">{{ profile.role || 'USER' }}</span>
          </div>
          <div class="v4-kv">
            <span class="k">注册时间</span>
            <span class="v v--mut">{{ formatTime(profile.createdAt) }}</span>
          </div>
          <div class="v4-kv kv-bio">
            <span class="k">文化签名</span>
            <span class="v v--mut">{{ profile.bio || '还没有写文化签名' }}</span>
          </div>
          <button
            type="button"
            class="v4-btn v4-btn--ghost v4-btn--sm v4-btn--block edit-open"
            @click="startEdit"
          >
            编辑资料
          </button>
        </template>

        <!-- 编辑态：PUT /api/v1/users/me（真实接口，只提交昵称与文化签名） -->
        <form v-else class="edit" @submit.prevent="saveProfile">
          <label class="edit-field">
            <span>昵称</span>
            <el-input v-model="form.nickname" maxlength="64" placeholder="请输入昵称" />
          </label>
          <label class="edit-field">
            <span>文化签名</span>
            <el-input
              v-model="form.bio"
              type="textarea"
              :rows="3"
              maxlength="255"
              show-word-limit
              placeholder="一句话表达你的营造志趣"
            />
          </label>
          <p v-if="saveError" class="edit-err" role="alert">{{ saveError }}</p>
          <div class="edit-acts">
            <button
              type="submit"
              class="v4-btn v4-btn--gold v4-btn--sm"
              :disabled="saving || !canSave"
            >
              {{ saving ? '保存中…' : '保存' }}
            </button>
            <button
              type="button"
              class="v4-btn v4-btn--ghost v4-btn--sm"
              :disabled="saving"
              @click="cancelEdit"
            >
              取消
            </button>
          </div>
        </form>
      </section>

      <!-- ═══════════ ③ 通知策略 · NOTIFY ═══════════ -->
      <section class="v4-card">
        <div class="v4-cardhd">
          <h3>通知策略</h3>
          <span class="k">NOTIFY</span>
        </div>

        <div v-for="p in PREF_ITEMS" :key="p.key" class="v4-switch-row">
          <span>
            {{ p.label }}
            <b>{{ p.hint }}</b>
          </span>
          <button
            type="button"
            class="v4-switch"
            :class="{ 'is-on': prefs[p.key] }"
            role="switch"
            :aria-checked="prefs[p.key]"
            :aria-label="p.label"
            @click="togglePref(p.key)"
          ></button>
        </div>

        <p class="v4-note">
          这些偏好保存在本机浏览器（localStorage），暂未同步到账号：后端目前没有通知偏好接口，
          换设备或清理浏览器数据后会回到默认值。
          「数字锦盒送达提醒」关闭后，幻筑完成不再弹窗，站内信仍会送达通知中心。
        </p>
      </section>

      <!-- ═══════════ ④ 关于平台 · ABOUT ═══════════ -->
      <section class="v4-card">
        <div class="v4-cardhd">
          <h3>关于平台</h3>
          <span class="k">ABOUT</span>
        </div>

        <div class="v4-kv">
          <span class="k">平台版本</span>
          <span class="v">智观·古建 v{{ appVersion }}</span>
        </div>
        <div class="v4-kv">
          <span class="k">前端技术栈</span>
          <span class="v">Vue 3 · Vite 5 · Element Plus</span>
        </div>
        <div class="v4-kv">
          <span class="k">解析引擎</span>
          <span class="v v--jade">VGGT-Long v2</span>
        </div>
        <div class="v4-kv">
          <span class="k">渲染管线</span>
          <span class="v">WebGL · PBR · Draco</span>
        </div>
        <div class="v4-kv">
          <span class="k">后端</span>
          <span class="v">Spring Boot 3.2 · MySQL · Redis</span>
        </div>

        <p class="v4-note">
          本项目为大学生创新创业训练计划（大创）作品。三维几何解析结果由 VGGT 自动生成，
          文化解读为 AI 转译，非人工考据。
        </p>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { authApi } from '@/api/auth'
import { useThemeStore } from '@/stores/theme'
import { useNotifyPrefs } from '@/composables/useNotifyPrefs'
import { version as appVersion } from '../../package.json'

const router = useRouter()
const themeStore = useThemeStore()

/* ═══════════ 账号资料 ═══════════ */

const profile = ref(null)
const profileLoading = ref(true)
const profileError = ref('')
const editing = ref(false)
const saving = ref(false)
const saveError = ref('')
const form = ref({ nickname: '', bio: '' })

const isAdmin = computed(() => profile.value?.role === 'ADMIN')
const canSave = computed(() => form.value.nickname.trim().length > 0)

function toErrorState(e) {
  const status = e?.response?.status
  if (status === 401) return '登录状态已失效，请重新登录后查看账号资料。'
  if (status === 403) return '当前账号没有权限查看这份资料。'
  return e?.response?.data?.message || e?.message || '网络异常，请检查后端服务是否已启动。'
}

/** 后端 createdAt = LocalDateTime.toString()（2024-05-01T10:23:45），此处只做展示格式化 */
function formatTime(raw) {
  if (!raw) return '—'
  return String(raw).replace('T', ' ').slice(0, 16)
}

async function loadProfile() {
  profileLoading.value = true
  profileError.value = ''
  try {
    const res = await authApi.getCurrentUser()
    profile.value = res?.data || {}
  } catch (e) {
    profile.value = null
    profileError.value = toErrorState(e)
  } finally {
    profileLoading.value = false
  }
}

function startEdit() {
  form.value = {
    nickname: profile.value?.nickname || '',
    bio: profile.value?.bio || ''
  }
  saveError.value = ''
  editing.value = true
}

function cancelEdit() {
  editing.value = false
  saveError.value = ''
}

async function saveProfile() {
  if (saving.value || !canSave.value) return
  saving.value = true
  saveError.value = ''
  try {
    // 真实接口：PUT /api/v1/users/me，只提交改动字段（avatarUrl 不在此处编辑）
    await authApi.updateProfile({
      nickname: form.value.nickname.trim(),
      bio: form.value.bio.trim()
    })
    editing.value = false
    await loadProfile() // 保存后以服务端为准刷新
    ElMessage.success('资料已更新')
  } catch (e) {
    saveError.value = e?.response?.data?.message || e?.message || '保存失败，请稍后重试'
  } finally {
    saving.value = false
  }
}

/* ═══════════ 通知策略（本机偏好）═══════════
   读写逻辑收敛到 composable：BrocadeDialog 要读同一份偏好，
   若此处再持一份 localStorage 逻辑就会有两个 owner 各写同一个 key。 */

const { prefs, items: PREF_ITEMS, toggle: togglePref } = useNotifyPrefs()

/* ═══════════ 通用 ═══════════ */

function goBack() {
  if (window.history.state?.back) router.back()
  else router.push('/home')
}

onMounted(loadProfile)
</script>

<style scoped>
/* ═══ 卡片内键值行：给紧凑的 .v4-kv 补一点行距 ═══ */
.v4-card > .v4-kv {
  padding: 5px 0;
}

/* 文化签名可能较长：顶部对齐、右侧换行，390px 下不横向溢出 */
.v4-card > .v4-kv.kv-bio {
  align-items: flex-start;
}

.v4-card > .v4-kv.kv-bio > .v {
  text-align: right;
  max-width: 62%;
  overflow-wrap: anywhere;
}

.edit-open {
  margin-top: var(--spacing-md);
}

/* ═══ 编辑态表单 ═══ */
.edit-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
}

.edit-field > span {
  font-size: 11px;
  color: var(--color-text-muted);
}

.edit-err {
  font-size: 11px;
  color: var(--color-rose-text);
  margin-bottom: 10px;
}

.edit-acts {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

/* ═══ 卡片内状态块（加载 / 错误）═══ */
.card-state {
  padding: 28px 16px;
}

.card-state > .v4-btn {
  margin-top: var(--spacing-md);
}

.sk-lines {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 4px 0;
}

.sk-line {
  height: 10px;
  border-radius: var(--radius-xs);
  background: var(--color-bg-subtle);
  animation: sk-pulse 1.4s ease-in-out infinite;
}

.sk-line--60 { width: 60%; }
.sk-line--85 { width: 85%; }

@keyframes sk-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
</style>
