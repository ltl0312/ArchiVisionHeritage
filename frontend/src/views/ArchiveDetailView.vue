<template>
  <div class="view-shell view-shell--flush">
    <!-- ═══════════ ① 加载中 ═══════════ -->
    <div v-if="loading && !post" class="arc-gate" role="status" aria-live="polite">
      <el-skeleton animated>
        <template #template>
          <el-skeleton-item variant="image" class="arc-skel-hd" />
          <div class="arc-skel-bd">
            <el-skeleton-item variant="h1" class="arc-skel-l1" />
            <el-skeleton-item variant="text" class="arc-skel-l2" />
            <el-skeleton-item variant="text" class="arc-skel-l3" />
          </div>
        </template>
      </el-skeleton>
      <p class="arc-skel-tip">
        <el-icon class="loading-spin" :size="14"><Loading /></el-icon>
        正在调取数字档案…
      </p>
    </div>

    <!-- ═══════════ ② 错误 / 档案不存在 ═══════════ -->
    <div v-else-if="!post" class="arc-gate">
      <div class="v4-empty">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M6 3h8l4 4v14H6z" />
          <path d="M14 3v5h4" />
        </svg>
        <b class="arc-empty-t">{{ errorTitle }}</b>
        <p class="arc-empty-d">{{ errorDetail }}</p>
      </div>
      <div class="arc-gate-acts">
        <button class="v4-btn v4-btn--gold" @click="load">重新加载</button>
        <button class="v4-btn v4-btn--ghost" @click="goCommunity">返回匠人社区</button>
      </div>
    </div>

    <!-- ═══════════ ③ 成功：沉浸详情 ═══════════ -->
    <div v-else class="v4-immers">
      <!-- ── 头图区 ── -->
      <div class="v4-immers-hd">
        <img v-if="post.preview2dPath" :src="post.preview2dPath" :alt="`${post.title} 实景封面`" />
        <div v-else class="arc-hd-ph">
          <el-icon :size="52"><PictureFilled /></el-icon>
          <span>该档案未上传实景封面</span>
        </div>

        <div class="v4-floatbar">
          <button class="v4-backpill" aria-label="返回上一页" @click="goBack">
            <el-icon :size="16"><ArrowLeft /></el-icon>
            返回
          </button>
          <div class="v4-fb-right">
            <button
              class="v4-iconbtn"
              type="button"
              aria-label="复制当前档案链接并分享"
              @click="handleShare"
            >
              <el-icon :size="18"><Share /></el-icon>
            </button>
            <button
              class="v4-iconbtn arc-fb-like"
              :class="{ 'is-on': post.likedByMe }"
              type="button"
              :aria-label="post.likedByMe ? '取消收藏点赞' : '收藏并点赞该档案'"
              :aria-pressed="post.likedByMe ? 'true' : 'false'"
              @click="handleLike"
            >
              <el-icon :size="18">
                <StarFilled v-if="post.likedByMe" />
                <Star v-else />
              </el-icon>
            </button>
          </div>
        </div>

        <div class="v4-immers-txt">
          <span v-if="headerTag" class="v4-dyn">{{ headerTag }}</span>
          <h1>{{ post.title || '未命名档案' }}</h1>
          <p>{{ headerSub }}</p>
          <div class="meta">{{ headerMeta }}</div>
        </div>
      </div>

      <!-- ── 正文区：左主列 + 右侧栏 ── -->
      <div class="v4-immers-body">
        <!-- 左列 -->
        <div class="arc-col">
          <div class="v4-vwrap">
            <!-- 三维查看器 -->
            <div class="v4-viewer">
              <!-- 有 GLB：库按需加载完成后渲染自定义元素 -->
              <model-viewer
                v-if="modelState === 'ready'"
                :src="modelSrc"
                :alt="`${post.title} 三维模型`"
                camera-controls
                auto-rotate
                auto-rotate-delay="1500"
                rotation-per-second="16deg"
                shadow-intensity="1"
                exposure="1"
                interaction-prompt="none"
                touch-action="pan-y"
                loading="lazy"
                reveal="auto"
                @error="modelFailed = true"
              ></model-viewer>

              <!-- 无 GLB / 加载中 / 加载失败：回退实景封面（无封面时留深色占位） -->
              <template v-else>
                <img
                  v-if="post.preview2dPath"
                  :src="post.preview2dPath"
                  :alt="`${post.title} 实景封面`"
                />
                <div class="v4-scrim v4-scrim--flat"></div>
                <div class="arc-viewer-ov">
                  <template v-if="modelState === 'loading'">
                    <el-icon class="loading-spin" :size="26"><Loading /></el-icon>
                    <span>正在按需加载三维查看器…</span>
                  </template>
                  <template v-else-if="modelState === 'failed'">
                    <el-icon :size="26"><WarningFilled /></el-icon>
                    <span>三维查看器加载失败</span>
                    <small>已回退实景封面</small>
                    <button class="v4-btn v4-btn--ghost v4-btn--sm" type="button" @click="retryModel">
                      <el-icon :size="14"><Refresh /></el-icon>
                      重试加载
                    </button>
                  </template>
                  <template v-else>
                    <el-icon :size="26"><Box /></el-icon>
                    <span>该档案暂无三维模型（仅实景解析）</span>
                    <small>
                      {{ post.preview2dPath ? '上方实景封面为该档案的二维影像' : '该档案同时未上传实景封面' }}
                    </small>
                  </template>
                </div>
              </template>

              <span
                v-if="modelState === 'ready'"
                class="v4-pill v4-pill--gold tag"
              >三维模型 · GLB</span>
              <span
                v-else-if="modelState === 'loading'"
                class="v4-pill v4-pill--jade tag"
              >三维模型 · 加载中</span>
              <span v-else-if="modelState === 'failed'" class="v4-pill v4-pill--red tag">三维模型 · 失败</span>
              <span v-else class="v4-pill v4-pill--mut tag">仅实景解析</span>

              <span v-if="viewerCap" class="cap">{{ viewerCap }}</span>
            </div>

            <!-- 模型信息 -->
            <div class="v4-card">
              <div class="v4-cardhd">
                <h3>模型信息</h3>
                <span class="k">本档案实际字段</span>
              </div>
              <div class="arc-kvs">
                <div class="v4-kv">
                  <span class="k">模型来源</span>
                  <span class="v" :class="post.glb3dPath ? 'v--jade' : 'v--mut'">
                    {{ post.glb3dPath ? 'GLB 模型文件' : '暂无三维模型' }}
                  </span>
                </div>
                <div class="v4-kv">
                  <span class="k">渲染方式</span>
                  <span class="v">{{ post.glb3dPath ? 'WebGL · model-viewer' : '实景封面（二维）' }}</span>
                </div>
                <div class="v4-kv">
                  <span class="k">模型文件</span>
                  <span class="v arc-ellip" :title="post.glb3dPath || ''">
                    {{ post.glb3dPath ? fileName(post.glb3dPath) : '—' }}
                  </span>
                </div>
                <div class="v4-kv">
                  <span class="k">交互方式</span>
                  <span class="v v--mut">{{ post.glb3dPath ? '拖拽旋转 · 自动环绕' : '—' }}</span>
                </div>
                <div class="v4-kv">
                  <span class="k">建档时间</span>
                  <span class="v v--mut">{{ post.createdAt || '—' }}</span>
                </div>
                <div class="v4-kv">
                  <span class="k">创建者</span>
                  <span class="v v--gold">{{ post.authorNickname || '匿名匠人' }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 档案正文 -->
          <div v-if="hasContent" class="v4-card">
            <div class="v4-cardhd">
              <h3>档案正文</h3>
              <span class="k">作者自述 · 未做考据加工</span>
            </div>
            <div class="v4-prose">
              <p class="arc-pre">{{ post.content }}</p>
            </div>
          </div>
          <div v-else class="v4-empty">该档案暂无正文说明，仅提供实景与模型数据</div>

          <!-- 文化探讨 -->
          <div class="v4-card">
            <div class="v4-cardhd">
              <h3>文化探讨</h3>
              <span class="k">{{ post.commentCount || 0 }} 条</span>
            </div>
            <div v-if="!userStore.isLoggedIn" class="v4-tip arc-login-tip">
              <b>匿名浏览中</b>
              登录后即可发表探讨、点赞与关注档案创建者。
              <button class="v4-btn v4-btn--outline v4-btn--sm" type="button" @click="goLogin">
                去登录
              </button>
            </div>
            <CommentList
              v-model="commentText"
              :comments="post.comments"
              :loading="loading"
              :is-logged-in="userStore.isLoggedIn"
              :submitting="submittingComment"
              @submit="submitComment"
            />
          </div>
        </div>

        <!-- 右列 -->
        <div class="arc-side">
          <div class="v4-card">
            <div class="v4-cardhd"><h3>形制档案</h3></div>
            <div class="arc-kvs">
              <div class="v4-kv">
                <span class="k">文化标签</span>
                <span class="v arc-ellip" :title="tags.join(' · ')">
                  {{ tags.length ? tags.join(' · ') : '未标注' }}
                </span>
              </div>
              <div class="v4-kv">
                <span class="k">标签数量</span>
                <span class="v v--mut">{{ tags.length }} 项</span>
              </div>
              <div class="v4-kv">
                <span class="k">三维模型</span>
                <span class="v" :class="post.glb3dPath ? 'v--jade' : 'v--mut'">
                  {{ post.glb3dPath ? '已挂载' : '未挂载' }}
                </span>
              </div>
              <div class="v4-kv">
                <span class="k">实景封面</span>
                <span class="v" :class="post.preview2dPath ? 'v--jade' : 'v--mut'">
                  {{ post.preview2dPath ? '已上传' : '未上传' }}
                </span>
              </div>
            </div>
          </div>

          <div class="v4-card v4-card--jade">
            <div class="v4-cardhd">
              <h3>数据溯源</h3>
              <span class="k">接口实际返回</span>
            </div>
            <div class="arc-kvs">
              <div class="v4-kv">
                <span class="k">档案编号</span>
                <span class="v v--jade">#{{ post.postId }}</span>
              </div>
              <div class="v4-kv">
                <span class="k">建档时间</span>
                <span class="v v--jade">{{ post.createdAt || '—' }}</span>
              </div>
              <div class="v4-kv">
                <span class="k">模型文件</span>
                <span class="v arc-ellip" :title="post.glb3dPath || ''">
                  {{ post.glb3dPath ? fileName(post.glb3dPath) : '无' }}
                </span>
              </div>
              <div class="v4-kv">
                <span class="k">封面文件</span>
                <span class="v arc-ellip" :title="post.preview2dPath || ''">
                  {{ post.preview2dPath ? fileName(post.preview2dPath) : '无' }}
                </span>
              </div>
              <div class="v4-kv">
                <span class="k">数据接口</span>
                <span class="v v--mut arc-ellip" :title="`/api/v1/posts/${post.postId}`">
                  /api/v1/posts/{{ post.postId }}
                </span>
              </div>
            </div>
          </div>

          <div class="v4-card arc-author">
            <el-avatar :size="40" :src="post.authorAvatarUrl || undefined" :icon="UserFilled" />
            <div class="arc-author-txt">
              <b>{{ post.authorNickname || '匿名匠人' }}</b>
              <span>档案创建者</span>
            </div>
            <button
              v-if="!isSelf"
              class="v4-btn v4-btn--outline arc-follow"
              type="button"
              :aria-label="post.followedByMe ? '取消关注该档案创建者' : '关注该档案创建者'"
              :aria-pressed="post.followedByMe ? 'true' : 'false'"
              :disabled="following"
              @click="handleFollow"
            >
              {{ post.followedByMe ? '已关注' : '关注' }}
            </button>
            <span v-else class="v4-pill v4-pill--mut v4-pill--bare arc-follow">本人</span>
          </div>

          <div v-if="tags.length" class="arc-chips">
            <span v-for="tag in tags" :key="tag" class="v4-chip">{{ tag }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  Share,
  Star,
  StarFilled,
  PictureFilled,
  UserFilled,
  Loading,
  Refresh,
  Box,
  WarningFilled
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { communityApi } from '@/api/community'
import { useUserStore } from '@/stores/user'
import { useOptimisticLike } from '@/composables/useOptimisticLike'
import { parseTags } from '@/utils/format'
import CommentList from '@/components/CommentList.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/* ═══ 数据与四态 ═══ */
const post = ref(null)
const loading = ref(true)
const errorTitle = ref('档案加载失败')
const errorDetail = ref('网络或服务暂时不可用，请稍后重试。')

/* ═══ 三维查看器状态：none（无 GLB）/ loading / ready / failed ═══ */
const modelSrc = ref('')
const modelReady = ref(false)
const modelFailed = ref(false)
let modelToken = 0

const modelState = computed(() => {
  if (!post.value?.glb3dPath) return 'none'
  if (modelFailed.value) return 'failed'
  if (modelReady.value && modelSrc.value) return 'ready'
  return 'loading'
})

/* ═══ 评论 ═══ */
const commentText = ref('')
const submittingComment = ref(false)
const following = ref(false)

const tags = computed(() => parseTags(post.value?.tags))
const hasContent = computed(() => !!post.value?.content && post.value.content.trim() !== '')

const isSelf = computed(
  () => userStore.userId != null && post.value?.authorId != null && userStore.userId === post.value.authorId
)

/** 头图丹砂标签：优先真实标签，其次真实建档时间 */
const headerTag = computed(() => {
  if (tags.value.length) return tags.value[0]
  if (post.value?.createdAt) return `建档 · ${post.value.createdAt}`
  return ''
})

const headerSub = computed(() => {
  const who = post.value?.authorNickname || '匿名匠人'
  const raw = (post.value?.content || '').replace(/\s+/g, ' ').trim()
  if (!raw) return `${who} · 该档案暂无正文说明`
  return `${who} · ${raw.length > 46 ? `${raw.slice(0, 46)}…` : raw}`
})

const headerMeta = computed(() => {
  const p = post.value
  if (!p) return ''
  const parts = [
    `点赞 ${p.likeCount || 0}`,
    `评论 ${p.commentCount || 0}`,
    tags.value.length ? `标签 ${tags.value.length} 项` : '未标注标签',
    p.glb3dPath ? '三维模型 1 件' : '暂无三维模型'
  ]
  if (p.createdAt) parts.push(`建档 ${p.createdAt}`)
  return parts.join(' · ')
})

const viewerCap = computed(() => {
  const p = post.value
  if (!p) return ''
  if (modelState.value === 'ready') return `GLB · ${fileName(p.glb3dPath)}`
  if (p.preview2dPath) return `实景封面 · ${fileName(p.preview2dPath)}`
  return ''
})

function fileName(path) {
  if (!path) return ''
  const s = String(path)
  const i = Math.max(s.lastIndexOf('/'), s.lastIndexOf('\\'))
  return i >= 0 ? s.slice(i + 1) : s
}

/* ═══ 加载档案 ═══ */
async function load() {
  disposeModel()
  loading.value = true
  const id = route.params.id
  try {
    const res = await communityApi.getPostDetail(id)
    const data = res?.data
    if (!data) {
      post.value = null
      errorTitle.value = '未找到这份数字档案'
      errorDetail.value = '服务端没有返回该编号的档案内容，它可能已被作者删除。'
      return
    }
    post.value = data
    commentText.value = ''
    prepareModel()
  } catch (e) {
    post.value = null
    const status = e?.response?.status
    if (status === 404) {
      errorTitle.value = '未找到这份数字档案'
      errorDetail.value = '该档案不存在，或已被作者删除。可返回匠人社区浏览其它档案。'
    } else if (status === 403) {
      errorTitle.value = '无法查看这份档案'
      errorDetail.value = '该档案未公开，当前账号没有查看权限。'
    } else if (status === 401) {
      errorTitle.value = '登录状态已失效'
      errorDetail.value = '请重新登录后再查看该档案。'
    } else {
      errorTitle.value = '档案加载失败'
      errorDetail.value = `请求未能完成${status ? `（HTTP ${status}）` : ''}，请检查网络后重试。`
    }
  } finally {
    loading.value = false
  }
}

/* ═══ 三维查看器：动态 import（不进主包），卸载即释放 ═══ */
async function prepareModel() {
  modelReady.value = false
  modelFailed.value = false
  modelSrc.value = ''
  const glb = post.value?.glb3dPath
  if (!glb) return

  const token = ++modelToken
  try {
    // 动态 import：按需加载，配合 vite 的分块把 @google/model-viewer 排除出主包
    await import('@google/model-viewer')
    if (token !== modelToken) return
    modelSrc.value = glb
    modelReady.value = true
  } catch {
    if (token !== modelToken) return
    modelFailed.value = true
  }
}

function retryModel() {
  modelFailed.value = false
  prepareModel()
}

/** 释放：先断开 src（卸载已载入的 GLB），再移除 <model-viewer> 元素 */
function disposeModel() {
  modelToken += 1
  modelSrc.value = ''
  modelReady.value = false
  modelFailed.value = false
}

/* ═══ 点赞 / 关注 / 分享 / 评论 ═══ */
const { toggle: toggleLike } = useOptimisticLike({
  getTarget: () => post.value,
  api: (p) => communityApi.toggleLike({ targetId: p.postId, targetType: 'POST' })
})

async function handleLike() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('登录后即可收藏与点赞')
    goLogin()
    return
  }
  await toggleLike()
  if (post.value?.likedByMe) ElMessage.success('已加入收藏')
}

async function handleFollow() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('登录后即可关注档案创建者')
    goLogin()
    return
  }
  if (isSelf.value || following.value) return
  const prev = post.value.followedByMe
  post.value.followedByMe = !prev
  following.value = true
  try {
    await communityApi.toggleFollow(post.value.authorId)
    ElMessage.success(post.value.followedByMe ? '已关注该匠人' : '已取消关注')
  } catch {
    post.value.followedByMe = prev
  } finally {
    following.value = false
  }
}

async function handleShare() {
  const url = window.location.href
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(url)
    } else {
      const ta = document.createElement('textarea')
      ta.value = url
      ta.setAttribute('readonly', '')
      ta.style.position = 'fixed'
      ta.style.top = '-1000px'
      document.body.appendChild(ta)
      ta.select()
      document.execCommand('copy')
      document.body.removeChild(ta)
    }
    ElMessage.success('档案链接已复制到剪贴板')
  } catch {
    ElMessage.warning('复制失败，请手动复制地址栏链接')
  }
}

async function submitComment() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('登录后即可参与探讨')
    goLogin()
    return
  }
  const content = commentText.value.trim()
  if (!content) return
  submittingComment.value = true
  try {
    const res = await communityApi.addComment(post.value.postId, { content })
    ElMessage.success('已发表探讨')
    if (!Array.isArray(post.value.comments)) post.value.comments = []
    post.value.comments.push(res.data)
    commentText.value = ''
    post.value.commentCount = (post.value.commentCount || 0) + 1
  } catch {
    /* 响应拦截器已统一提示 */
  } finally {
    submittingComment.value = false
  }
}

/* ═══ 导航 ═══ */
function goBack() {
  if (window.history.state && window.history.state.back) router.back()
  else router.push('/community')
}

function goCommunity() {
  router.push('/community')
}

function goLogin() {
  router.push('/login')
}

onMounted(load)
onBeforeUnmount(disposeModel)

watch(
  () => route.params.id,
  (id, prev) => {
    if (id !== prev) load()
  }
)
</script>

<style scoped>
/* ═══ 状态门（flush 外壳下自带内边距） ═══ */
.arc-gate {
  padding: var(--spacing-xl);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.arc-skel-hd {
  width: 100%;
  height: 220px;
  border-radius: var(--radius-lg);
}

.arc-skel-bd {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: var(--spacing-lg);
}

.arc-skel-l1 { width: 42%; height: 26px; }
.arc-skel-l2 { width: 72%; height: 14px; }
.arc-skel-l3 { width: 56%; height: 14px; }

.arc-skel-tip {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-top: var(--spacing-md);
  font-size: var(--font-size-caption);
  color: var(--color-text-muted);
}

.arc-empty-t {
  display: block;
  margin-bottom: 6px;
  font-family: var(--font-family-serif);
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-main);
}

.arc-empty-d {
  font-size: var(--font-size-caption);
  line-height: 1.8;
  color: var(--color-text-muted);
}

.arc-gate-acts {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
}

/* ═══ 头图占位 ═══ */
.arc-hd-ph {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: var(--color-panel-deep);
  color: var(--color-text-faint);
  font-size: var(--font-size-caption);
}

/* ═══ 正文两列 ═══ */
.arc-col {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}

.arc-side {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  min-width: 0;
}

.arc-kvs {
  display: flex;
  flex-direction: column;
  gap: 11px;
}

.arc-ellip {
  max-width: 58%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.arc-pre {
  white-space: pre-wrap;
  word-break: break-word;
}

/* ═══ 三维查看器 ═══ */
.v4-viewer :deep(model-viewer) {
  display: block;
  width: 100%;
  height: 100%;
  background-color: var(--color-panel-deep);
}

/* 头图标题最长 3 行，避免超长标题把 pill / 元信息顶出 380px 头图区被裁掉 */
.v4-immers-txt h1 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.arc-viewer-ov {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 0 var(--spacing-md);
  text-align: center;
  font-size: var(--font-size-caption);
  color: var(--color-on-media-dim);
}

.arc-viewer-ov > small {
  font-size: var(--font-size-meta);
  color: var(--color-on-media-faint);
}

.arc-viewer-ov .v4-btn { margin-top: var(--spacing-xs); }

.arc-fb-like.is-on {
  color: var(--color-accent-light);
  border-color: var(--color-border-gold);
}

/* ═══ 侧栏：作者卡与标签 ═══ */
.arc-author {
  display: flex;
  align-items: center;
  gap: 12px;
}

.arc-author-txt {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.arc-author-txt > b {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-main);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.arc-author-txt > span {
  font-size: var(--font-size-meta);
  color: var(--color-text-faint);
}

.arc-follow {
  flex: 0 0 auto;
  height: 32px;
  padding: 0 14px;
  font-size: var(--font-size-meta);
  border-radius: var(--radius-full);
}

.arc-chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
}

.arc-chips .v4-chip {
  cursor: default;
  pointer-events: none;
}

/* ═══ 评论区未登录提示 ═══ */
.arc-login-tip {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: var(--spacing-md);
}

.arc-login-tip > b {
  display: inline;
  margin-bottom: 0;
}

@media (max-width: 900px) {
  .arc-skel-hd { height: 160px; }
  .arc-ellip { max-width: 62%; }
}
</style>
