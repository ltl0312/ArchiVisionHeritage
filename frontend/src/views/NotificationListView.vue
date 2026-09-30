<template>
  <div class="view-shell">
    <!-- ═══ 工具行：返回 + 未读统计 + 全部已读 ═══ -->
    <div class="v4-toolrow">
      <button type="button" class="v4-btn v4-btn--ghost v4-btn--sm" @click="goBack">
        <el-icon :size="14"><ArrowLeft /></el-icon>返回
      </button>

      <div class="v4-headstats">
        <span class="v4-pill v4-pill--red">未读 {{ unreadCount }} 条</span>
        <button
          type="button"
          class="v4-btn v4-btn--ghost v4-btn--sm"
          :disabled="unreadCount === 0 || markingAll"
          @click="markAllRead"
        >
          {{ markingAll ? '处理中…' : '全部标记已读' }}
        </button>
      </div>
    </div>

    <!-- ═══ 分类筛选（chip，与幻筑页标签语言一致）═══
         后端 NotificationResponse 无 type / category 字段，
         唯一可推导的分类维度是 taskId 是否为空：
         taskId != null → 幻筑成果（数字锦盒）；taskId == null → 系统消息。 -->
    <div class="v4-chips filters" role="group" aria-label="通知分类筛选">
      <button
        v-for="c in CHIPS"
        :key="c.value"
        type="button"
        class="v4-chip"
        :class="{ 'is-active': filter === c.value }"
        :aria-pressed="filter === c.value"
        @click="filter = c.value"
      >
        {{ c.label }}
      </button>
    </div>

    <!-- ① 加载中：骨架行 ═══ -->
    <div v-if="loading" class="v4-notif-list" aria-busy="true" aria-live="polite">
      <div v-for="i in 3" :key="i" class="sk-row">
        <span class="sk-ico"></span>
        <span class="sk-lines">
          <span class="sk-line sk-line--40"></span>
          <span class="sk-line sk-line--90"></span>
          <span class="sk-line sk-line--25"></span>
        </span>
      </div>
    </div>

    <!-- ② 错误：说明 + 重试 ═══ -->
    <div v-else-if="error" class="v4-empty" role="alert">
      <svg viewBox="0 0 40 40" aria-hidden="true">
        <path d="M20 8L34 32H6L20 8Z" />
        <path d="M20 17.5V24.5" />
        <path d="M20 28.4H20.02" />
      </svg>
      <p>{{ error }}</p>
      <small>通知列表未能加载，可点击下方按钮重试。</small>
      <button type="button" class="v4-btn v4-btn--outline v4-btn--sm" @click="load">重试</button>
    </div>

    <!-- ③ 空：真的一条通知都没有 ═══ -->
    <div v-else-if="notifications.length === 0" class="v4-empty">
      <svg viewBox="0 0 40 40" aria-hidden="true">
        <path d="M10 17.5C10 11.5 14.4 7 20 7C25.6 7 30 11.5 30 17.5V25L33.5 29H6.5L10 25V17.5Z" />
        <path d="M16.5 32.5C17.2 34 18.5 34.8 20 34.8C21.5 34.8 22.8 34 23.5 32.5" />
      </svg>
      <p>还没有通知</p>
      <small>一键幻筑任务完成后，数字锦盒会在这里送达。</small>
    </div>

    <!-- ④ 分类空：有通知，但当前分类下没有 ═══ -->
    <div v-else-if="filtered.length === 0" class="v4-empty">
      <svg viewBox="0 0 40 40" aria-hidden="true">
        <path d="M10 17.5C10 11.5 14.4 7 20 7C25.6 7 30 11.5 30 17.5V25L33.5 29H6.5L10 25V17.5Z" />
        <path d="M16.5 32.5C17.2 34 18.5 34.8 20 34.8C21.5 34.8 22.8 34 23.5 32.5" />
      </svg>
      <p>该分类下暂无通知</p>
      <small>其他分类中还有 {{ notifications.length }} 条通知。</small>
      <button type="button" class="v4-btn v4-btn--outline v4-btn--sm" @click="filter = 'all'">
        查看全部
      </button>
    </div>

    <!-- ⑤ 列表 ═══ -->
    <div v-else class="v4-notif-list">
      <div
        v-for="n in filtered"
        :key="n.id"
        class="v4-notif-row"
        :class="{ 'is-unread': !n.read }"
      >
        <!-- 行内主点击区：真实 button（避免 button 内嵌套 button），点条目即已读 -->
        <button
          type="button"
          class="nmain"
          :aria-label="n.read ? `已读通知：${titleOf(n)}` : `标记为已读：${titleOf(n)}`"
          @click="markRead(n)"
        >
          <span class="v4-nicon" :class="n.taskId != null ? 'v4-nicon--gold' : 'v4-nicon--jade'">
            <el-icon :size="18">
              <Present v-if="n.taskId != null" />
              <Bell v-else />
            </el-icon>
          </span>
          <span class="v4-nbody">
            <b>{{ titleOf(n) }}</b>
            <p>{{ n.message }}</p>
            <span class="t">{{ n.createdAt }}</span>
          </span>
        </button>

        <div class="v4-nact">
          <!-- 只有数字锦盒有下游动作（去「我的档案」查看幻筑成果） -->
          <button
            v-if="n.taskId != null"
            type="button"
            class="v4-btn v4-btn--gold v4-btn--sm"
            @click="openBrocade()"
          >
            打开锦盒
          </button>
          <span v-if="!n.read" class="v4-unread-dot" aria-hidden="true"></span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Bell, Present } from '@element-plus/icons-vue'
import { notificationApi } from '@/api/notification'
import { useNotificationStore } from '@/stores/notification'

const router = useRouter()
const notifStore = useNotificationStore()

/**
 * 分类 chip —— 只做后端契约支持得起的 3 类。
 * NotificationResponse 只有 {id, taskId, message, isRead, createdAt}，
 * 没有 type / category；当前唯一的生产者是 TaskCompletedEventListener（幻筑完成站内信），
 * 因此「审核结果 / 互动」没有任何数据源，做出来会永远是空的死 UI，故不实现。
 */
const CHIPS = [
  { value: 'all', label: '全部' },
  { value: 'box', label: '数字锦盒' },
  { value: 'system', label: '系统消息' }
]

const filter = ref('all')
const notifications = ref([])
const loading = ref(true)
const error = ref('')
const markingAll = ref(false)

const unreadCount = computed(() => notifStore.unreadCount)

const filtered = computed(() => {
  if (filter.value === 'box') return notifications.value.filter(n => n.taskId != null)
  if (filter.value === 'system') return notifications.value.filter(n => n.taskId == null)
  return notifications.value
})

/**
 * 归一化「已读」标记。
 * 后端 NotificationResponse.isRead 是 boolean isRead（Lombok 生成 isRead()），
 * Jackson 序列化后的键名是 read；两种键名都兼容，缺省按未读处理。
 */
function normalize(n) {
  const raw = n?.isRead !== undefined ? n.isRead : n?.read
  return { ...n, read: raw === undefined ? false : !!raw }
}

/** 标题由 taskId 推导（后端无 title 字段，message 只作正文） */
function titleOf(n) {
  return n.taskId != null ? '数字锦盒已送达' : '系统消息'
}

function toErrorState(e) {
  const status = e?.response?.status
  if (status === 401) return '登录状态已失效，请重新登录后查看通知。'
  if (status === 403) return '当前账号没有权限查看通知。'
  return e?.response?.data?.message || e?.message || '网络异常，请检查后端服务是否已启动。'
}

/**
 * 数据加载：直连 notificationApi.list()。
 * 不用 store.fetchNotifications()，因为它在 store 内部 try/catch 吞掉异常，
 * 视图层无法区分「一条都没有」与「请求失败」——而本页必须给出真实错误态。
 * 加载后调用 store.fetchUnreadCount()，让顶栏铃铛与「未读 N 条」取服务端真值。
 */
async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await notificationApi.list()
    const list = Array.isArray(res?.data) ? res.data : []
    notifications.value = list.map(normalize)
    await notifStore.fetchUnreadCount()
  } catch (e) {
    notifications.value = []
    error.value = toErrorState(e)
  } finally {
    loading.value = false
  }
}

/** 点条目即已读：视图层同步列表项，store 同步 unreadCount（store 不动列表项，故在此补齐） */
async function markRead(n) {
  if (n.read) return
  n.read = true
  try {
    await notifStore.markAsRead(n.id)
  } catch {
    n.read = false // 失败回滚（拦截器已统一提示）
  }
}

async function markAllRead() {
  if (markingAll.value || unreadCount.value === 0) return
  markingAll.value = true
  try {
    await notifStore.markAllRead()
    notifications.value.forEach(n => { n.read = true })
    ElMessage.success('已全部标为已读')
  } catch {
    /* 拦截器已统一提示 */
  } finally {
    markingAll.value = false
  }
}

/** 数字锦盒的下游动作 = 去「我的档案」查看幻筑成果（通知只带 taskId，无 assetId 可深链） */
function openBrocade() {
  router.push('/archive')
}

function goBack() {
  if (window.history.state?.back) router.back()
  else router.push('/home')
}

onMounted(load)
</script>

<style scoped>
/* ═══ 筛选行 ═══ */
.filters {
  margin-bottom: var(--spacing-md);
}

/* ═══ 行内主点击区（图标 + 正文），是真实 button ═══ */
.nmain {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  flex: 1;
  min-width: 0;
  padding: 0;
  background: none;
  border: none;
  font: inherit;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.nmain:focus-visible {
  outline: 2px solid var(--color-accent);
  outline-offset: 3px;
  border-radius: var(--radius-sm);
}

/* 长 message / 长词不撑破 390px 窄屏 */
.v4-nbody > b,
.v4-nbody > p {
  overflow-wrap: anywhere;
}

/* ═══ 空态 / 错误态内部的补充行 ═══ */
.v4-empty > p {
  font-size: 13px;
  color: var(--color-text-muted);
}

.v4-empty > small {
  display: block;
  margin-top: 6px;
  font-size: 11px;
  color: var(--color-text-faint);
}

.v4-empty > .v4-btn {
  margin-top: var(--spacing-md);
}

/* ═══ 加载骨架 ═══ */
.sk-row {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px;
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
}

.sk-ico {
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
  border-radius: var(--radius-md);
  background: var(--color-bg-subtle);
}

.sk-lines {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sk-line {
  height: 10px;
  border-radius: var(--radius-xs);
  background: var(--color-bg-subtle);
}

.sk-line--40 { width: 40%; }
.sk-line--90 { width: 90%; }
.sk-line--25 { width: 25%; }

.sk-ico,
.sk-line {
  animation: sk-pulse 1.4s ease-in-out infinite;
}

@keyframes sk-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
</style>
