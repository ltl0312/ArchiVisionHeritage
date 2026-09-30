<template>
  <div class="view-shell">
    <!-- ═══════════ 工具行：返回 + 状态胶囊 ═══════════ -->
    <div class="v4-toolrow">
      <router-link class="v4-btn v4-btn--ghost v4-btn--sm" to="/community">
        <el-icon :size="14"><ArrowLeft /></el-icon>
        返回匠人社区
      </router-link>

      <div class="v4-headstats">
        <span class="v4-pill v4-pill--gold">待审核 {{ pendingTotal }}</span>
        <span class="v4-pill v4-pill--jade">本次会话已通过 {{ sessionApproved }}</span>
        <span class="v4-pill v4-pill--red">本次会话已驳回 {{ sessionRejected }}</span>
      </div>
    </div>

    <div class="v4-admin-grid">
      <!-- ═══════════ 左列：待审队列 ═══════════ -->
      <div class="admin-main">
        <div class="v4-card">
          <div class="v4-cardhd">
            <h3>待审队列</h3>
            <span class="k">按提交时间正序 · 先到先审</span>
          </div>

          <!-- 加载中 -->
          <div v-if="loading" class="v4-audit-list" aria-busy="true" aria-label="正在加载待审队列">
            <div v-for="n in 3" :key="n" class="v4-audit-row">
              <el-skeleton animated class="skel-row">
                <template #template>
                  <el-skeleton-item variant="image" class="skel-thumb" />
                  <div class="skel-txt">
                    <el-skeleton-item variant="h3" style="width: 52%" />
                    <el-skeleton-item variant="text" style="width: 34%" />
                  </div>
                </template>
              </el-skeleton>
            </div>
          </div>

          <!-- 无权限（接口 403）：路由守卫之外的第二道说明，避免白屏 -->
          <div v-else-if="forbidden" class="v4-empty" role="alert">
            <el-icon :size="40" class="empty-ico"><Lock /></el-icon>
            <p>需要管理员权限</p>
            <p class="empty-sub">当前账号无权访问内容审核队列。请以掌印官（管理员）账号登录，或返回匠人社区浏览公开内容。</p>
            <router-link class="v4-btn v4-btn--ghost v4-btn--sm" to="/community">返回匠人社区</router-link>
          </div>

          <!-- 错误 -->
          <div v-else-if="error" class="v4-empty" role="alert">
            <el-icon :size="40" class="empty-ico"><WarningFilled /></el-icon>
            <p>待审队列加载失败</p>
            <p class="empty-sub">{{ error }}</p>
            <button type="button" class="v4-btn v4-btn--outline v4-btn--sm" @click="loadPending()">
              重试
            </button>
          </div>

          <!-- 空态 -->
          <div v-else-if="!queue.length" class="v4-empty">
            <el-icon :size="40" class="empty-ico"><CircleCheck /></el-icon>
            <p>待审队列已清空 · 全部内容已处理</p>
            <p class="empty-sub">新提交的作品会在此出现，先到先审。</p>
          </div>

          <!-- 正常 -->
          <template v-else>
            <div class="v4-audit-list">
              <article v-for="post in queue" :key="post.postId" class="v4-audit-row">
                <div class="thumb">
                  <img
                    v-if="post.preview2dPath && !brokenThumbs.has(post.postId)"
                    :src="post.preview2dPath"
                    :alt="`${post.title || '未命名档案'} 的 2D 预览缩略图`"
                    width="64"
                    height="48"
                    loading="lazy"
                    decoding="async"
                    @error="markThumbBroken(post.postId)"
                  />
                  <span v-else class="thumb-ph" aria-hidden="true">
                    <el-icon :size="20"><PictureRounded /></el-icon>
                  </span>
                </div>

                <div class="info">
                  <b>{{ post.title || '未命名档案' }}</b>
                  <span class="meta">{{ metaLine(post) }}</span>
                </div>

                <div class="acts">
                  <button
                    type="button"
                    class="v4-btn v4-btn--gold v4-btn--sm"
                    :disabled="busyId !== null"
                    :aria-label="`通过《${post.title || '未命名档案'}》`"
                    @click="approve(post)"
                  >
                    {{ busyId === post.postId && !rejectingId ? '提交中…' : '通过' }}
                  </button>
                  <button
                    type="button"
                    class="v4-btn v4-btn--ghost v4-btn--sm act-reject"
                    :disabled="busyId !== null"
                    :aria-expanded="rejectingId === post.postId"
                    :aria-label="`驳回《${post.title || '未命名档案'}》并填写理由`"
                    @click="openReject(post.postId)"
                  >
                    驳回
                  </button>
                </div>

                <!-- 驳回理由：空理由在 UI 层被拦截，规则前置 -->
                <div v-if="rejectingId === post.postId" class="reject-box">
                  <label class="reject-lab" :for="`reject-reason-${post.postId}`">驳回理由（必填）</label>
                  <input
                    :id="`reject-reason-${post.postId}`"
                    :ref="el => setRejectInput(post.postId, el)"
                    v-model="rejectReason"
                    type="text"
                    aria-label="驳回理由，必填，将同步至作者站内信"
                    placeholder="写明具体原因，将同步至作者站内信"
                    maxlength="200"
                    @keyup.enter="confirmReject(post)"
                  />
                  <button
                    type="button"
                    class="v4-btn v4-btn--gold v4-btn--sm"
                    :disabled="busyId !== null || !rejectReason.trim()"
                    @click="confirmReject(post)"
                  >
                    {{ busyId === post.postId ? '提交中…' : '确认驳回' }}
                  </button>
                  <button
                    type="button"
                    class="v4-btn v4-btn--ghost v4-btn--sm"
                    :disabled="busyId !== null"
                    @click="closeReject"
                  >
                    取消
                  </button>
                  <p class="reject-hint">理由将同步至作者站内信 · 留空无法提交</p>
                </div>
              </article>
            </div>

            <div v-if="pendingTotal > PAGE_SIZE" class="queue-page">
              <el-pagination
                background
                layout="prev, pager, next"
                :current-page="page"
                :page-size="PAGE_SIZE"
                :total="pendingTotal"
                @current-change="onPageChange"
              />
            </div>
          </template>
        </div>
      </div>

      <!-- ═══════════ 右列：审核要点 + 最近操作 ═══════════ -->
      <div class="admin-side">
        <div class="v4-card">
          <div class="v4-cardhd">
            <h3>审核要点</h3>
            <span class="k">RUBRIC</span>
          </div>
          <ul class="v4-rules">
            <li>影像须为古建实拍或本平台 AI 生成，不得含第三方水印</li>
            <li>标题不得使用夸大或营销化表述</li>
            <li>涉及具体文保单位须标注地域与朝代</li>
            <li>驳回必须填写理由，理由将同步至作者站内信</li>
          </ul>
        </div>

        <div class="v4-card v4-card--jade">
          <div class="v4-cardhd">
            <h3>最近操作 · 本次会话</h3>
            <span class="k">AUDIT LOG</span>
          </div>
          <div v-if="sessionLog.length" class="v4-loglist">
            <div v-for="row in recentLog" :key="row.id" class="v4-logrow">
              <span class="act">{{ row.act }}</span>
              <b :title="row.title">{{ row.title }}</b>
              <span class="t">{{ row.time }}</span>
            </div>
          </div>
          <p v-else class="log-empty">
            本次会话尚无操作记录。审核动作会即时记在这里，刷新页面即清空。
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import {
  ArrowLeft,
  CircleCheck,
  Lock,
  PictureRounded,
  WarningFilled
} from '@element-plus/icons-vue'
import { adminApi } from '@/api/admin'
import { recordsFallback } from '@/utils/pagination'
import { parseTags } from '@/utils/format'

/**
 * 审核工作台（设计稿 §4.1「队列 + 侧栏」）—— 逐条决策的动作流，不是批量比对的数据流。
 *
 * 数据源（后端仅有两个接口，无统计、无审核历史）：
 *   · GET /api/v1/admin/posts/pending?page&size → Page<PostBriefResponse>
 *   · PUT /api/v1/admin/posts/{id}/audit         → { status: 'APPROVED' | 'REJECTED', rejectReason? }
 *
 * 因此：
 *   · 「待审核 N」= pendingPage.total（真实）
 *   · 「本次会话已通过 / 已驳回」= 本地会话计数（后端无统计接口，标签明确写「本次会话」，不伪装成「今日」）
 *   · 「最近操作」只记录本会话内自己执行过的动作（后端无审核历史接口，不预填示例日志）
 */

const PAGE_SIZE = 20

/* ═══ 队列状态 ═══ */
const queue = ref([])
const page = ref(1)
const pendingTotal = ref(0)
const loading = ref(true)
const error = ref('')
const forbidden = ref(false)

/* ═══ 会话内计数与日志（后端无对应接口，纯本地） ═══ */
const sessionApproved = ref(0)
const sessionRejected = ref(0)
const sessionLog = ref([])

/* ═══ 交互态 ═══ */
const busyId = ref(null)          // 正在提交的行（通过/驳回共用，防重复提交）
const rejectingId = ref(null)     // 展开驳回理由框的行
const rejectReason = ref('')
const rejectInputs = new Map()    // postId → input 元素，用于展开后自动聚焦
const brokenThumbs = ref(new Set()) // 缩略图 404 的行，回退为占位图标

const recentLog = computed(() => sessionLog.value.slice(0, 8))

/* ═══ 格式化 ═══ */
function formatDate(raw) {
  if (!raw) return ''
  const s = String(raw).replace('T', ' ')
  const m = s.match(/^(\d{4})-(\d{2})-(\d{2})/)
  return m ? `${m[1]}-${m[2]}-${m[3]}` : s.slice(0, 16)
}

function formatTime(raw) {
  if (!raw) return ''
  const s = String(raw).replace('T', ' ')
  const m = s.match(/\d{2}:\d{2}/)
  return m ? m[0] : ''
}

/** 行元信息：作者 · 提交时间 · 标签（全部来自 PostBriefResponse 真实字段） */
function metaLine(post) {
  const tags = parseTags(post.tags).join(' / ')
  return [
    post.authorNickname || '匿名匠人',
    formatDate(post.createdAt),
    tags
  ].filter(Boolean).join(' · ')
}

function markThumbBroken(postId) {
  brokenThumbs.value = new Set(brokenThumbs.value).add(postId)
}

function setRejectInput(postId, el) {
  if (el) rejectInputs.set(postId, el)
  else rejectInputs.delete(postId)
}

function pushLog(act, title) {
  sessionLog.value.unshift({
    id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    act,
    title: title || '未命名档案',
    time: formatTime(new Date().toISOString())
  })
}

/* ═══ 加载（四态：loading / forbidden / error / 空 / 正常） ═══ */
async function loadPending() {
  loading.value = true
  error.value = ''
  forbidden.value = false
  try {
    const res = await adminApi.getPendingPosts(page.value, PAGE_SIZE)
    const data = res?.data
    queue.value = recordsFallback(data, [])
    pendingTotal.value = typeof data?.total === 'number' ? data.total : queue.value.length
    // 当前页被审空且不是第一页 → 回退一页（只在这里改 page，不会触发 watch 递归）
    if (!queue.value.length && page.value > 1) {
      page.value -= 1
      return await loadPending()
    }
  } catch (e) {
    queue.value = []
    pendingTotal.value = 0
    if (e?.response?.status === 403) {
      forbidden.value = true
    } else {
      error.value = e?.response?.data?.message || e?.message || '网络异常，请检查后端服务是否已启动。'
    }
  } finally {
    loading.value = false
  }
}

function onPageChange(p) {
  page.value = p
  rejectingId.value = null
  rejectReason.value = ''
  loadPending()
}

/* ═══ 审核动作 ═══ */
async function approve(post) {
  if (busyId.value !== null) return
  busyId.value = post.postId
  try {
    await adminApi.auditPost(post.postId, 'APPROVED')
    sessionApproved.value += 1
    pushLog('通过', post.title)
    if (rejectingId.value === post.postId) closeReject()
    await loadPending()
  } catch { /* 拦截器已统一提示，保持静默 */ } finally {
    busyId.value = null
  }
}

async function openReject(postId) {
  if (busyId.value !== null) return
  if (rejectingId.value === postId) { closeReject(); return }
  rejectingId.value = postId
  rejectReason.value = ''
  await nextTick()
  rejectInputs.get(postId)?.focus()
}

function closeReject() {
  rejectingId.value = null
  rejectReason.value = ''
}

async function confirmReject(post) {
  if (busyId.value !== null) return
  // UI 层强制：空理由不得提交（不依赖后端校验）
  if (!rejectReason.value.trim()) return
  busyId.value = post.postId
  try {
    await adminApi.auditPost(post.postId, 'REJECTED', rejectReason.value.trim())
    sessionRejected.value += 1
    pushLog('驳回', post.title)
    closeReject()
    await loadPending()
  } catch { /* 拦截器已统一提示 */ } finally {
    busyId.value = null
  }
}

onMounted(loadPending)
</script>

<style scoped>
/* ═══ 左列 / 右列（.v4-admin-grid 已在 style.css 定义，≤900px 降为单列） ═══ */
.admin-main {
  min-width: 0;
}
.admin-side {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

/* ═══ 缩略图占位（preview2dPath 为空或加载失败时） ═══ */
.thumb-ph {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  color: var(--color-text-ghost);
  background: var(--color-scrim-soft);
}

/* ═══ 骨架（占位尺寸与 .v4-audit-row 对齐，避免加载完成时跳动） ═══ */
.skel-row {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
}
.skel-thumb {
  width: 64px;
  height: 48px;
  flex: 0 0 64px;
  border-radius: var(--radius-xs);
}
.skel-txt {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* ═══ 空态 / 无权限 / 错误（复用全局 .v4-empty，仅补图标与副文案） ═══ */
/* Element Plus 图标是 <i><svg fill="currentColor">，需覆盖全局 .v4-empty > svg 的描边规则 */
.v4-empty .empty-ico {
  display: block;
  margin: 0 auto 12px;
  color: var(--color-text-ghost);
}
.v4-empty .empty-ico svg {
  stroke: none;
  fill: currentColor;
}
.v4-empty > p {
  font-size: 13px;
  color: var(--color-text-muted);
}
.v4-empty .empty-sub {
  margin: 6px 0 18px;
  font-size: 12px;
  line-height: 1.75;
  color: var(--color-text-faint);
  max-width: 420px;
  margin-left: auto;
  margin-right: auto;
}

/* ═══ 驳回按钮：丹砂语义（破坏性动作），与「通过」的金色行动色区分 ═══ */
.act-reject {
  color: var(--color-rose-text);
  border-color: var(--color-border-rose);
}
.act-reject:hover {
  color: var(--color-rose-text);
  border-color: var(--color-rose);
  background: var(--color-rose-soft);
}

/* ═══ 驳回理由框（结构由 .v4-audit-row > .reject-box 提供） ═══ */
.reject-lab {
  flex: 0 0 auto;
  font-size: var(--font-size-meta);
  color: var(--color-rose-text);
}
.reject-hint {
  flex-basis: 100%;
  font-size: 10px;
  color: var(--color-text-faint);
}

/* ═══ 分页 ═══ */
.queue-page {
  display: flex;
  justify-content: center;
  margin-top: var(--spacing-lg);
}

/* ═══ 会话日志空态 ═══ */
.log-empty {
  font-size: 11.5px;
  line-height: 1.75;
  color: var(--color-text-faint);
}

/* ═══ 本视图专属窄屏微调（全局 ≤900px 已处理 .v4-admin-grid 与 .acts 换行） ═══ */
@media (max-width: 900px) {
  .reject-lab {
    flex-basis: 100%;
  }
  .v4-audit-row > .reject-box > input {
    flex-basis: 100%;
  }
}
</style>
