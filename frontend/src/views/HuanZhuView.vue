<script>
/**
 * 模块级轮询守卫（刻意放在 <script setup> 之外的模块作用域）
 *
 * 背景（D-8 缺陷）：原实现的 pollTaskStatus() 每次调用都会新建一条
 * setTimeout 递归链，却不清理上一条；重复点击「开始幻筑」会并发多条链持续打接口。
 *
 * 修复机制 = 「单定时器 + generation token」双重保险：
 *   1. pollTimer 是全模块唯一的定时器句柄：任何新链建立前先 clearTimeout，
 *      并立即重新赋值 —— 结构上不可能同时存在两个待触发的定时器。
 *   2. pollToken 是自增的代数（generation）：每轮新提交 +1，回调闭包捕获自己那代的
 *      token；异步续跑前先比对 token !== pollToken 就直接 return，不再排下一次定时器。
 *      因此即使旧链正处于「await 网络往返」中（此刻 clearTimeout 已无从拦截），
 *      它复活后也只会自尽，无法再造定时器。
 * 结论：只有最新一代的链能创建定时器 → 任何时刻最多一条活跃轮询链。
 * 放在模块作用域是为了让守卫跨组件实例/热更新共享（本项目未使用 keep-alive，
 * 路由离开时 onUnmounted 会清定时器并把 token 失效）。
 */
let pollTimer = null
let pollToken = 0
let pollCount = 0
let elapsedTimer = null

export default { name: 'HuanZhuView' }
</script>

<script setup>
import { ref, computed, watch, onUnmounted } from 'vue'
import {
  MagicStick, Loading, Box, Lock, Warning, Clock,
  CopyDocument, FullScreen, RefreshRight, RefreshLeft
} from '@element-plus/icons-vue'
import { huanzhuApi } from '@/api/huanzhu'
import { useUserStore } from '@/stores/user'
import { useNotificationStore } from '@/stores/notification'
import { ElMessage } from 'element-plus'

/* ═══ 常量（与后端 / 设计稿对齐） ═══ */
const MAX_LEN = 200          // 提示词上限（设计稿 maxlength="200"）
const MAX_POLL = 60          // 最多轮询 60 次
const POLL_INTERVAL = 2000   // 轮询间隔 2s（沿用既有节奏）
const POLL_WINDOW_SEC = Math.round((MAX_POLL * POLL_INTERVAL) / 1000)

const DYNASTY_TAGS = ['唐代', '宋代', '明清']
const ROOF_TAGS = ['歇山顶', '庑殿顶', '琉璃黄', '朱红大漆']

const userStore = useUserStore()
const notifStore = useNotificationStore()

/* ═══ 状态 ═══ */
const prompt = ref('')
const submitting = ref(false)      // 已发出 submit，尚未拿到任务状态
const status = ref(null)           // 后端真实状态：PENDING / RUNNING / SUCCESS / FAILED
const taskId = ref(null)
const taskResult = ref(null)       // TaskStatusResponse 全量（含 preview2dPath / glb3dPath / assetId）
const errorMessage = ref('')       // FAILED 时后端返回的 errorMessage
const timedOut = ref(false)        // 轮询次数用尽
const needLogin = ref(false)       // 未登录 / 401
const imgError = ref(false)        // 封面图加载失败
const elapsedSec = ref(0)          // 本地真实计时（从提交时刻起算）
const selected = ref({ dynasty: [], roof: [] })

/* ═══ 派生状态 ═══ */
const viewState = computed(() => {
  if (needLogin.value) return 'unauth'
  if (status.value === 'SUCCESS') return 'success'
  if (status.value === 'FAILED') return 'failed'
  if (timedOut.value) return 'timeout'
  if (submitting.value) return 'submitting'
  if (status.value === 'RUNNING') return 'running'
  if (status.value === 'PENDING') return 'pending'
  return 'idle'
})

const busy = computed(() =>
  !timedOut.value &&
  (submitting.value || status.value === 'PENDING' || status.value === 'RUNNING')
)

const previewUrl = computed(() => taskResult.value?.preview2dPath || '')
const glbName = computed(() => taskResult.value?.glb3dPath?.split('/').pop() || '')

const submitLabel = computed(() => {
  switch (viewState.value) {
    case 'submitting': return '正在提交任务…'
    case 'pending': return '已入队 · 等待受理'
    case 'running': return '三维生成引擎演算中…'
    case 'success': return '重新幻筑'
    case 'failed':
    case 'timeout': return '重新提交'
    case 'unauth': return '请先登录'
    default: return '生成三维幻境'
  }
})

const statusText = computed(() => {
  switch (viewState.value) {
    case 'unauth': return '未登录 · 无法提交任务'
    case 'submitting': return '正在提交 · 入队中…'
    case 'pending': return `已提交 · 等待受理 · 已等待 ${elapsedSec.value}s`
    case 'running': return `三维生成引擎演算中 · 已等待 ${elapsedSec.value}s`
    case 'success': return '资产入库 · 锦盒送达'
    case 'failed': return '营造失败'
    case 'timeout': return '轮询超时 · 请查看通知'
    default: return '视口待命 · 尚无任务'
  }
})

const statusPillClass = computed(() => {
  switch (viewState.value) {
    case 'success': return 'v4-pill--jade'
    case 'failed':
    case 'timeout':
    case 'unauth': return 'v4-pill--red'
    case 'idle': return 'v4-pill--mut'
    default: return 'v4-pill--gold'
  }
})

/** 右胶囊 = 资产信息，只展示后端真实返回的 assetId / glb3dPath 是否就位 */
const assetText = computed(() => {
  if (viewState.value === 'success' && taskResult.value) {
    const parts = []
    if (taskResult.value.assetId) parts.push(`资产 #${taskResult.value.assetId}`)
    parts.push(taskResult.value.glb3dPath ? 'GLB 已入库' : '封面已生成')
    return parts.join(' · ')
  }
  return viewState.value === 'idle' ? '暂无三维资产' : '资产未生成'
})

const assetPillClass = computed(() =>
  viewState.value === 'success' ? 'v4-pill--jade' : 'v4-pill--mut'
)

/**
 * 工序时间轴 = 后端真实状态机（PENDING → RUNNING → SUCCESS / FAILED）。
 * 后端没有子工序进度字段，因此这里只点亮真实状态，不做假的百分比与假工序。
 * RUNNING / PENDING 的「已等待 Ns」是前端本地计时，可核查。
 */
const steps = computed(() => {
  const s = status.value
  if (!s && !submitting.value) return []
  const wait = `已等待 ${elapsedSec.value}s`

  if (s === 'SUCCESS') {
    return [
      { t: '已提交 · 等待受理', s: '完成', cls: 'is-done' },
      { t: '三维生成引擎演算中', s: '完成', cls: 'is-done' },
      { t: '资产入库 · 锦盒送达', s: '已入库', cls: 'is-done' }
    ]
  }
  if (s === 'FAILED') {
    return [
      { t: '已提交 · 等待受理', s: '完成', cls: 'is-done' },
      { t: '三维生成引擎演算中', s: '失败', cls: 'is-err' },
      { t: '资产入库 · 锦盒送达', s: '未执行', cls: '' }
    ]
  }
  if (s === 'RUNNING') {
    return [
      { t: '已提交 · 等待受理', s: '完成', cls: 'is-done' },
      { t: '三维生成引擎演算中', s: wait, cls: 'is-now' },
      { t: '资产入库 · 锦盒送达', s: '待执行', cls: '' }
    ]
  }
  if (s === 'PENDING') {
    return [
      { t: '已提交 · 等待受理', s: wait, cls: 'is-now' },
      { t: '三维生成引擎演算中', s: '待执行', cls: '' },
      { t: '资产入库 · 锦盒送达', s: '待执行', cls: '' }
    ]
  }
  return [
    { t: '已提交 · 等待受理', s: '提交中', cls: 'is-now' },
    { t: '三维生成引擎演算中', s: '待执行', cls: '' },
    { t: '资产入库 · 锦盒送达', s: '待执行', cls: '' }
  ]
})

/* ═══ 轮询守卫 ═══ */
function stopPollTimer() {
  if (pollTimer) {
    clearTimeout(pollTimer)
    pollTimer = null
  }
}

/** 让所有在途回调失效，并清掉唯一的定时器 —— 新提交前必须先调用 */
function invalidatePolling() {
  pollToken += 1
  stopPollTimer()
}

function startElapsed() {
  stopElapsed()
  elapsedSec.value = 0
  elapsedTimer = setInterval(() => { elapsedSec.value += 1 }, 1000)
}

function stopElapsed() {
  if (elapsedTimer) {
    clearInterval(elapsedTimer)
    elapsedTimer = null
  }
}

/* ═══ 标签（真实写入 / 移除提示词） ═══ */
function toggleTag(group, tag) {
  const list = selected.value[group]
  const idx = list.indexOf(tag)

  if (idx === -1) {
    // 用户已手写该文化要素：只标记选中，不重复追加
    if (prompt.value.includes(tag)) {
      list.push(tag)
      return
    }
    const cur = prompt.value.trim()
    const next = cur ? `${cur}，${tag}` : tag
    if (next.length > MAX_LEN) {
      ElMessage.warning(`提示词已达 ${MAX_LEN} 字上限，未能追加「${tag}」`)
      return
    }
    prompt.value = next
    list.push(tag)
    return
  }

  list.splice(idx, 1)
  removeTagFromPrompt(tag)
}

function removeTagFromPrompt(tag) {
  const segs = prompt.value.split('，')
  const idx = segs.findIndex(seg => seg.trim() === tag)
  if (idx !== -1) {
    segs.splice(idx, 1)
    prompt.value = segs.join('，')
    return
  }
  // 兜底：标签文本出现在句子中间（用户手写），移除首个出现并清理多余分隔符
  const pos = prompt.value.indexOf(tag)
  if (pos === -1) return
  const next = (prompt.value.slice(0, pos) + prompt.value.slice(pos + tag.length))
    .replace(/^，+/, '')
    .replace(/，{2,}/g, '，')
    .replace(/，\s*$/, '')
    .trim()
  prompt.value = next
}

/* 用户在输入框里手删标签文本时，同步取消对应 chip 的选中态 */
watch(prompt, (v) => {
  for (const group of ['dynasty', 'roof']) {
    selected.value[group] = selected.value[group].filter(tag => v.includes(tag))
  }
})

/* ═══ 提交 + 轮询 ═══ */
async function handleGenerate() {
  const text = prompt.value.trim()
  if (!text || busy.value) return

  // 未登录：给出明确入口，不浪费一次 401 往返
  if (!userStore.isLoggedIn) {
    needLogin.value = true
    ElMessage.warning('请先登录后再提交幻筑任务')
    return
  }

  // 关键：任何新提交前，先清掉上一条定时器并让旧代回调失效
  invalidatePolling()
  const token = pollToken
  pollCount = 0

  submitting.value = true
  status.value = null
  taskId.value = null
  taskResult.value = null
  errorMessage.value = ''
  timedOut.value = false
  needLogin.value = false
  imgError.value = false
  startElapsed()

  try {
    const res = await huanzhuApi.submit(text)
    if (token !== pollToken) return // 已被更新的提交取代

    const id = res.data?.taskId
    // 幂等命中：Redis 锁返回的是「已有任务」，不是新任务 —— 继续同步它的进度
    if (res.data?.duplicate === true) {
      ElMessage.info('检测到相同描述已在处理中，正在同步其进度')
    }
    if (!id) {
      submitting.value = false
      stopElapsed()
      errorMessage.value = '服务端未返回任务号，请稍后重试'
      status.value = 'FAILED'
      return
    }

    taskId.value = id
    status.value = 'PENDING'
    submitting.value = false
    schedulePoll(id, token)
  } catch (e) {
    if (token !== pollToken) return
    submitting.value = false
    stopElapsed()
    if (e?.response?.status === 401) {
      needLogin.value = true
      status.value = null
      ElMessage.warning('请先登录后再提交幻筑任务')
      return
    }
    status.value = 'FAILED'
    errorMessage.value = e?.response?.data?.message || e?.message || '提交失败，请稍后重试'
  }
}

function schedulePoll(id, token) {
  stopPollTimer() // 结构保证：同一时刻至多一个待触发定时器
  pollTimer = setTimeout(() => pollOnce(id, token), POLL_INTERVAL)
}

async function pollOnce(id, token) {
  pollTimer = null
  if (token !== pollToken) return // 旧代回调直接自尽，绝不复活

  pollCount += 1
  if (pollCount > MAX_POLL) {
    timedOut.value = true
    stopElapsed()
    ElMessage.warning('任务处理超时，请稍后查看通知')
    return
  }

  try {
    const res = await huanzhuApi.getTaskStatus(id)
    if (token !== pollToken) return // 网络往返期间已被更新的提交取代

    const st = res.data?.status
    status.value = st

    if (st === 'SUCCESS') {
      taskResult.value = res.data // 含 preview2dPath / glb3dPath / assetId
      stopElapsed()
      // 「数字锦盒」送达弹窗：后端已在事务提交后写入站内信，
      // 这里只负责把它送达给用户（弹窗与否受「通知策略」偏好控制）。
      notifStore.showBrocade = true
      notifStore.fetchUnreadCount()
      return
    }
    if (st === 'FAILED') {
      errorMessage.value = res.data?.errorMessage || '营造引擎返回失败，未生成资产'
      stopElapsed()
      return
    }
    schedulePoll(id, token)
  } catch (e) {
    if (token !== pollToken) return
    if (e?.response?.status === 401) {
      needLogin.value = true
      stopElapsed()
      return
    }
    if (e?.response?.status === 404) {
      status.value = 'FAILED'
      errorMessage.value = '任务不存在或已过期，请重新提交'
      stopElapsed()
      return
    }
    // 网络抖动：继续轮询，仍受 MAX_POLL 次数上限约束
    schedulePoll(id, token)
  }
}

/* ═══ 视口工具条（真实动作，不做假的模式切换） ═══ */
async function copyPrompt() {
  const text = prompt.value.trim()
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('提示词已复制')
  } catch {
    ElMessage.warning('浏览器未授予剪贴板权限，请手动复制')
  }
}

function openPreview() {
  if (previewUrl.value) window.open(previewUrl.value, '_blank', 'noopener')
}

function resetView() {
  invalidatePolling()
  stopElapsed()
  submitting.value = false
  status.value = null
  taskId.value = null
  taskResult.value = null
  errorMessage.value = ''
  timedOut.value = false
  needLogin.value = false
  imgError.value = false
  elapsedSec.value = 0
}

onUnmounted(() => {
  invalidatePolling() // 清定时器 + 让在途回调失效
  stopElapsed()
})
</script>

<template>
  <div class="view-shell">
    <div class="v4-bench v4-bench--2">
      <!-- ═══ 左列 · 创作区 ═══ -->
      <div class="v4-col v4-compose">
        <div class="v4-textarea">
          <textarea
            v-model="prompt"
            :maxlength="MAX_LEN"
            :disabled="busy"
            placeholder="请描绘您心中的殿宇，例如：唐代风格的重檐歇山顶大殿，配有绿琉璃瓦与朱红色的回廊……"
            aria-label="幻筑提示词"
          ></textarea>
          <div class="foot">
            <span>点击下方标签可快速补充文化要素</span>
            <span>已输入 {{ prompt.length }} / {{ MAX_LEN }}</span>
          </div>
        </div>

        <div class="v4-fieldset">
          <span class="lab">朝代风格</span>
          <div class="v4-chips">
            <button
              v-for="tag in DYNASTY_TAGS"
              :key="tag"
              type="button"
              class="v4-chip"
              :class="{ 'is-active': selected.dynasty.includes(tag) }"
              :aria-pressed="selected.dynasty.includes(tag)"
              @click="toggleTag('dynasty', tag)"
            >{{ tag }}</button>
          </div>
        </div>

        <div class="v4-fieldset">
          <span class="lab">屋顶形制与色彩倾向</span>
          <div class="v4-chips">
            <button
              v-for="tag in ROOF_TAGS"
              :key="tag"
              type="button"
              class="v4-chip"
              :class="{ 'is-active': selected.roof.includes(tag) }"
              :aria-pressed="selected.roof.includes(tag)"
              @click="toggleTag('roof', tag)"
            >{{ tag }}</button>
          </div>
        </div>

        <div class="v4-fieldset">
          <span class="lab">生成参数 · PARAMETERS（服务端固定，只读）</span>
          <div class="v4-opt"><span>文化词库增强</span><span class="opt-v">服务端自动 · AncientDictUtil</span></div>
          <div class="v4-opt"><span>三维生成耗时</span><span class="opt-v">异步管线 · 3–7 秒</span></div>
          <div class="v4-opt"><span>幂等守护</span><span class="opt-v">Redis 锁 · 同描述 10 分钟内复用</span></div>
          <div class="v4-opt"><span>轮询策略</span><span class="opt-v">2 秒 / 次 · 上限 {{ MAX_POLL }} 次</span></div>
        </div>

        <button
          type="button"
          class="v4-btn v4-btn--gold v4-btn--block v4-btn--lg"
          :disabled="!prompt.trim() || busy"
          @click="handleGenerate"
        >
          <el-icon v-if="busy" class="loading-spin"><Loading /></el-icon>
          <el-icon v-else><MagicStick /></el-icon>
          <span>{{ submitLabel }}</span>
        </button>
        <p class="submit-note">提交后进入异步队列，完成时以「数字锦盒」通知</p>
      </div>

      <!-- ═══ 右列 · 三维视口 ═══ -->
      <div class="v4-viewport v4-zone">
        <img
          v-if="viewState === 'success' && previewUrl && !imgError"
          :src="previewUrl"
          alt="AI 生成的三维殿宇封面"
          @load="imgError = false"
          @error="imgError = true"
        />
        <div class="v4-vp-scrim" aria-hidden="true"></div>

        <svg class="v4-corner v4-corner--tl" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M0.5 10V0.5H10" stroke="currentColor" /></svg>
        <svg class="v4-corner v4-corner--tr" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M14 0.5H23.5V10" stroke="currentColor" /></svg>
        <svg class="v4-corner v4-corner--bl" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M0.5 14V23.5H10" stroke="currentColor" /></svg>
        <svg class="v4-corner v4-corner--br" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M14 23.5H23.5V14" stroke="currentColor" /></svg>

        <div class="v4-vp-top">
          <span class="v4-pill" :class="statusPillClass">{{ statusText }}</span>
          <span class="v4-pill" :class="assetPillClass">{{ assetText }}</span>
        </div>

        <div v-if="glbName" class="v4-vp-meta">三维资产 · {{ glbName }}</div>

        <div
          v-if="steps.length"
          class="v4-craft"
          :class="{ 'is-failed': viewState === 'failed' }"
        >
          <div class="lab">任务状态 · TASK</div>
          <div v-for="step in steps" :key="step.t" class="v4-step" :class="step.cls">
            <span class="d"></span>
            <span class="t">{{ step.t }}</span>
            <span class="s">{{ step.s }}</span>
          </div>
        </div>

        <div class="vp-center">
          <!-- 初始态：不放假图片 -->
          <div v-if="viewState === 'idle'" class="vp-note">
            <el-icon :size="34" class="ic"><Box /></el-icon>
            <p class="tt">等待营造指令</p>
            <p class="ds">在左侧描绘殿宇形制，或用标签补充朝代与屋顶要素后提交。生成完成后封面与 GLB 会写入「数字锦盒」。</p>
          </div>

          <div v-else-if="viewState === 'submitting'" class="vp-note">
            <el-icon :size="30" class="ic loading-spin"><Loading /></el-icon>
            <p class="tt">正在提交营造指令</p>
            <p class="ds">已进入异步队列，正在获取任务号…</p>
          </div>

          <div v-else-if="viewState === 'success' && imgError" class="vp-note vp-note--error">
            <el-icon :size="30" class="ic"><Warning /></el-icon>
            <p class="tt">封面图未能加载</p>
            <p class="ds">任务已成功，但封面路径无法访问：{{ previewUrl }}</p>
          </div>

          <div v-else-if="viewState === 'failed'" class="vp-note vp-note--error">
            <el-icon :size="30" class="ic"><Warning /></el-icon>
            <p class="tt">营造失败</p>
            <p class="ds">{{ errorMessage || '服务端未返回失败原因，请稍后重试。' }}</p>
            <div class="acts">
              <button type="button" class="v4-btn v4-btn--gold" :disabled="!prompt.trim()" @click="handleGenerate">
                <el-icon><RefreshRight /></el-icon>
                <span>重试</span>
              </button>
            </div>
          </div>

          <div v-else-if="viewState === 'timeout'" class="vp-note">
            <el-icon :size="30" class="ic"><Clock /></el-icon>
            <p class="tt">任务处理超时</p>
            <p class="ds">已轮询 {{ MAX_POLL }} 次（约 {{ POLL_WINDOW_SEC }} 秒）仍未收到终态。任务可能仍在后端执行，完成时会写入「数字锦盒」。</p>
            <div class="acts">
              <router-link class="v4-btn v4-btn--outline" to="/notifications">查看通知中心</router-link>
              <button type="button" class="v4-btn v4-btn--ghost" :disabled="!prompt.trim()" @click="handleGenerate">重新提交</button>
            </div>
          </div>

          <div v-else-if="viewState === 'unauth'" class="vp-note vp-note--error">
            <el-icon :size="30" class="ic"><Lock /></el-icon>
            <p class="tt">请先登录</p>
            <p class="ds">提交幻筑任务需要登录账号（未授权时后端返回 401）。</p>
            <div class="acts">
              <router-link class="v4-btn v4-btn--gold" to="/login">前往登录</router-link>
            </div>
          </div>
        </div>

        <div class="v4-dock">
          <button
            type="button"
            title="复制当前提示词"
            aria-label="复制当前提示词"
            :disabled="!prompt.trim()"
            @click="copyPrompt"
          ><el-icon><CopyDocument /></el-icon></button>
          <button
            type="button"
            title="在新标签页查看封面原图"
            aria-label="在新标签页查看封面原图"
            :disabled="viewState !== 'success' || !previewUrl"
            @click="openPreview"
          ><el-icon><FullScreen /></el-icon></button>
          <button
            type="button"
            title="用当前提示词重新提交"
            aria-label="用当前提示词重新提交"
            :disabled="!prompt.trim() || busy"
            @click="handleGenerate"
          ><el-icon><RefreshRight /></el-icon></button>
          <button
            type="button"
            title="重置视口到等待状态"
            aria-label="重置视口到等待状态"
            :disabled="viewState === 'idle'"
            @click="resetView"
          ><el-icon><RefreshLeft /></el-icon></button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 视图专属微调。
   栅格降级（.v4-bench--2 → 单列）、视口保底高度、时间轴与角落金线的移动端规则
   已在 style.css 的全局 @media (max-width: 900px) 区块统一处理，此处不再重复。 */

/* 标签组容器（style.css 未提供 .v4-chips，仅提供 .v4-chip 单品） */
.v4-chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
}

/* 只读参数行的值列（对应设计稿的 v--jade） */
.opt-v {
  font-size: 11px;
  font-weight: 500;
  color: var(--color-jade-text);
}

.submit-note {
  margin-top: calc(var(--spacing-xs) * -1);
  font-size: 10px;
  line-height: 1.7;
  text-align: center;
  color: var(--color-text-faint);
}

.v4-textarea > textarea:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ═══ 角落金线（全局 .v4-corner 只定位，颜色由此处给令牌） ═══ */
.v4-corner {
  color: var(--color-accent);
  opacity: 0.4;
}
.v4-corner path {
  stroke-width: 1;
}

/* ═══ 视口中央态 ═══ */
.vp-center {
  position: absolute;
  inset: 0;
  z-index: 3;
  display: grid;
  place-items: center;
  padding: 88px 20px 80px;
  pointer-events: none;
}
.vp-note {
  pointer-events: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-sm);
  max-width: 420px;
  padding: var(--spacing-lg);
  border-radius: var(--radius-lg);
  background: var(--color-scrim-strong);
  border: 1px solid var(--color-media-line);
  text-align: center;
}
.vp-note--error {
  border-color: var(--color-rose);
}
.vp-note > .ic {
  color: var(--color-on-media-faint);
}
.vp-note--error > .ic {
  color: var(--color-rose-text);
}
.vp-note > .tt {
  font-family: var(--font-family-serif);
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: var(--color-on-media-strong);
}
.vp-note > .ds {
  font-size: 12px;
  line-height: 1.8;
  color: var(--color-on-media-dim);
  word-break: break-all;
}
.vp-note > .acts {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--spacing-sm);
  margin-top: var(--spacing-xs);
}

/* 成功态的三维资产文件名（影像上的青色元信息） */
.v4-vp-meta {
  max-width: 58%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ═══ 时间轴失败态（全局只提供 is-done / is-now） ═══ */
.v4-craft.is-failed {
  border-color: var(--color-rose);
}
.v4-step.is-err {
  color: var(--color-rose-text);
  font-weight: 500;
}
.v4-step.is-err > .d {
  background: var(--color-rose);
  border-color: var(--color-rose);
}
.v4-step.is-err > .s {
  color: var(--color-rose-text);
}

/* ═══ 视口工具条 ═══ */
.v4-dock > button[disabled] {
  opacity: 0.32;
  cursor: not-allowed;
}
.v4-dock > button[disabled]:hover {
  background: none;
}

/* 本视图专属的窄屏微调（断点与全局一致，统一 900px） */
@media (max-width: 900px) {
  .vp-center {
    align-items: end;
    padding: 76px 14px 76px;
  }
  .vp-note {
    padding: var(--spacing-md);
    gap: var(--spacing-xs);
  }
  .vp-note > .tt {
    font-size: 15px;
  }
  .v4-vp-top > .v4-pill {
    height: 28px;
    padding: 0 10px;
    font-size: 10px;
  }
  .v4-vp-meta {
    max-width: 66%;
    font-size: 9.5px;
  }
}
</style>
