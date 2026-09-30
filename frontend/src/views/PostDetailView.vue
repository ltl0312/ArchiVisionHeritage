<template>
  <div class="view-shell view-shell--flush">
    <!-- ═══════════ ① 加载中 ═══════════ -->
    <div v-if="loading && !post" class="pd-gate" role="status" aria-live="polite">
      <el-skeleton animated>
        <template #template>
          <el-skeleton-item variant="image" class="pd-skel-hd" />
          <div class="pd-skel-bd">
            <el-skeleton-item variant="h1" class="pd-skel-l1" />
            <el-skeleton-item variant="text" class="pd-skel-l2" />
            <el-skeleton-item variant="text" class="pd-skel-l3" />
          </div>
        </template>
      </el-skeleton>
      <p class="pd-skel-tip">
        <el-icon class="loading-spin" :size="14"><Loading /></el-icon>
        正在调取档案详情…
      </p>
    </div>

    <!-- ═══════════ ② 错误 / 404 ═══════════ -->
    <div v-else-if="!post" class="pd-gate">
      <div class="v4-empty">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M6 3h8l4 4v14H6z" />
          <path d="M14 3v5h4" />
        </svg>
        <b class="pd-empty-t">{{ errorTitle }}</b>
        <p class="pd-empty-d">{{ errorDetail }}</p>
      </div>
      <div class="pd-gate-acts">
        <button class="v4-btn v4-btn--gold" type="button" @click="load">重新加载</button>
        <button class="v4-btn v4-btn--ghost" type="button" @click="goCommunity">返回社区</button>
      </div>
    </div>

    <!-- ═══════════ ③ 成功：沉浸详情 ═══════════ -->
    <div v-else class="v4-immers">
      <!-- ── 头图区 ── -->
      <div class="v4-immers-hd">
        <img
          v-if="post.preview2dPath && !coverBroken"
          :src="post.preview2dPath"
          :alt="`${post.title || '档案'} 封面影像`"
          @error="coverBroken = true"
        />
        <div v-else class="pd-hd-ph">
          <div class="v4-scrim" aria-hidden="true"></div>
          <div class="pd-hd-ph-in">
            <el-icon :size="52"><PictureFilled /></el-icon>
            <span>该档案未上传封面影像</span>
          </div>
        </div>

        <div class="v4-floatbar">
          <button class="v4-backpill" type="button" aria-label="返回匠人社区" @click="goBack">
            <el-icon :size="16"><ArrowLeft /></el-icon>
            返回社区
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
        <div class="pd-col">
          <!-- 作者与标签 -->
          <div class="v4-card">
            <div class="pd-author">
              <el-avatar :size="44" :src="post.authorAvatarUrl || undefined" :icon="UserFilled" />
              <div class="pd-author-txt">
                <b>{{ post.authorNickname || '匿名匠人' }}</b>
                <span>{{ post.createdAt || '—' }} 发布</span>
              </div>
              <button
                v-if="!isSelf"
                class="v4-btn v4-btn--outline pd-follow"
                type="button"
                :aria-label="post.followedByMe ? '取消关注该档案作者' : '关注该档案作者'"
                :aria-pressed="post.followedByMe ? 'true' : 'false'"
                :disabled="following"
                @click="handleFollow"
              >
                {{ post.followedByMe ? '已关注' : '关注作者' }}
              </button>
              <span v-else class="v4-pill v4-pill--mut v4-pill--bare pd-follow">本人</span>
            </div>

            <div v-if="tags.length" class="pd-chips">
              <span v-for="tag in tags" :key="tag" class="v4-chip pd-chip">{{ tag }}</span>
            </div>
          </div>

          <!-- 档案正文（后端返回 Quill HTML，按纯文本渲染，不做 v-html） -->
          <div v-if="hasContent" class="v4-card">
            <div class="v4-cardhd">
              <h3>档案正文</h3>
              <span class="k">作者自述 · 未做考据加工</span>
            </div>
            <div class="v4-prose">
              <p class="pd-pre">{{ post.content }}</p>
            </div>
          </div>
          <div v-else class="v4-empty">该档案暂无正文说明，仅提供封面影像与互动记录</div>

          <!-- 互动 + 文化探讨 -->
          <div class="v4-card">
            <div class="v4-cardhd">
              <h3>文化探讨</h3>
              <span class="k">{{ post.commentCount || 0 }} 条</span>
            </div>

            <div class="pd-interact">
              <button
                class="v4-btn v4-btn--outline pd-like"
                :class="{ 'is-on': post.likedByMe }"
                type="button"
                :aria-label="post.likedByMe ? '取消赞赏该档案' : '赞赏该档案'"
                :aria-pressed="post.likedByMe ? 'true' : 'false'"
                @click="handleLike"
              >
                <el-icon :size="16">
                  <StarFilled v-if="post.likedByMe" />
                  <Star v-else />
                </el-icon>
                {{ post.likedByMe ? '已赞赏' : '赞赏' }} · {{ post.likeCount || 0 }}
              </button>
              <span class="v4-pill v4-pill--mut v4-pill--bare">
                探讨 {{ post.commentCount || 0 }} 条
              </span>
            </div>

            <div v-if="!userStore.isLoggedIn" class="v4-tip pd-login-tip">
              <b>匿名浏览中</b>
              登录后即可赞赏、关注作者与发表探讨。
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
        <div class="pd-side">
          <div class="v4-card">
            <div class="v4-cardhd">
              <h3>档案数据</h3>
              <span class="k">接口实际返回</span>
            </div>
            <div class="pd-kvs">
              <div class="v4-kv">
                <span class="k">档案编号</span>
                <span class="v v--jade">#{{ post.postId }}</span>
              </div>
              <div class="v4-kv">
                <span class="k">建档时间</span>
                <span class="v v--mut">{{ post.createdAt || '—' }}</span>
              </div>
              <div class="v4-kv">
                <span class="k">赞赏</span>
                <span class="v v--gold">{{ post.likeCount || 0 }}</span>
              </div>
              <div class="v4-kv">
                <span class="k">探讨</span>
                <span class="v">{{ post.commentCount || 0 }}</span>
              </div>
              <div class="v4-kv">
                <span class="k">文化标签</span>
                <span class="v pd-ellip" :title="tags.join(' · ')">
                  {{ tags.length ? tags.join(' · ') : '未标注' }}
                </span>
              </div>
              <div class="v4-kv">
                <span class="k">封面影像</span>
                <span class="v" :class="post.preview2dPath ? 'v--jade' : 'v--mut'">
                  {{ post.preview2dPath ? '已上传' : '未上传' }}
                </span>
              </div>
              <div class="v4-kv">
                <span class="k">三维模型</span>
                <span class="v" :class="post.glb3dPath ? 'v--jade' : 'v--mut'">
                  {{ post.glb3dPath ? '已挂载' : '未挂载' }}
                </span>
              </div>
            </div>
          </div>

          <div class="v4-card v4-card--jade">
            <div class="v4-cardhd">
              <h3>数据溯源</h3>
              <span class="k">详情接口</span>
            </div>
            <div class="pd-kvs">
              <div class="v4-kv">
                <span class="k">数据接口</span>
                <span class="v v--mut pd-ellip" :title="`/api/v1/posts/${post.postId}`">
                  /api/v1/posts/{{ post.postId }}
                </span>
              </div>
              <div class="v4-kv">
                <span class="k">封面文件</span>
                <span class="v pd-ellip" :title="post.preview2dPath || ''">
                  {{ post.preview2dPath ? fileName(post.preview2dPath) : '无' }}
                </span>
              </div>
              <div class="v4-kv">
                <span class="k">模型文件</span>
                <span class="v pd-ellip" :title="post.glb3dPath || ''">
                  {{ post.glb3dPath ? fileName(post.glb3dPath) : '无' }}
                </span>
              </div>
            </div>
            <router-link
              v-if="post.glb3dPath"
              class="v4-btn v4-btn--outline v4-btn--block pd-model-link"
              :to="`/archive/${post.postId}`"
            >
              查看三维模型
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  Share,
  Star,
  StarFilled,
  PictureFilled,
  UserFilled,
  Loading
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
const coverBroken = ref(false)
const errorTitle = ref('档案加载失败')
const errorDetail = ref('网络或服务暂时不可用，请稍后重试。')

/* ═══ 评论 / 关注 ═══ */
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
    `赞赏 ${p.likeCount || 0}`,
    `探讨 ${p.commentCount || 0}`,
    tags.value.length ? `标签 ${tags.value.length} 项` : '未标注标签',
    p.glb3dPath ? '三维模型 1 件' : '暂无三维模型'
  ]
  if (p.createdAt) parts.push(`建档 ${p.createdAt}`)
  return parts.join(' · ')
})

function fileName(path) {
  if (!path) return ''
  const s = String(path)
  const i = Math.max(s.lastIndexOf('/'), s.lastIndexOf('\\'))
  return i >= 0 ? s.slice(i + 1) : s
}

/* ═══ 加载详情（自持 try/catch，useAsyncAction 会吞掉错误，拿不到错误态） ═══ */
async function load() {
  loading.value = true
  coverBroken.value = false
  try {
    const res = await communityApi.getPostDetail(route.params.id)
    const data = res?.data
    if (!data) {
      post.value = null
      errorTitle.value = '未找到这份档案'
      errorDetail.value = '服务端没有返回该编号的档案内容，它可能已被作者删除。'
      return
    }
    post.value = data
    commentText.value = ''
  } catch (e) {
    post.value = null
    const status = e?.response?.status
    if (status === 404) {
      errorTitle.value = '未找到这份档案'
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

/* ═══ 点赞（乐观更新）/ 关注 / 分享 / 评论 ═══ */
const { toggle: toggleLike } = useOptimisticLike({
  getTarget: () => post.value,
  api: (p) => communityApi.toggleLike({ targetId: p.postId, targetType: 'POST' })
})

async function handleLike() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('登录后即可赞赏该档案')
    goLogin()
    return
  }
  await toggleLike()
}

async function handleFollow() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('登录后即可关注该档案作者')
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

watch(
  () => route.params.id,
  (id, prev) => {
    if (id !== prev) load()
  }
)
</script>

<style scoped>
/* ═══ 状态门（flush 外壳下自带内边距） ═══ */
.pd-gate {
  padding: var(--spacing-xl);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.pd-skel-hd {
  width: 100%;
  height: 220px;
  border-radius: var(--radius-lg);
}

.pd-skel-bd {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: var(--spacing-lg);
}

.pd-skel-l1 { width: 42%; height: 26px; }
.pd-skel-l2 { width: 72%; height: 14px; }
.pd-skel-l3 { width: 56%; height: 14px; }

.pd-skel-tip {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-top: var(--spacing-md);
  font-size: var(--font-size-caption);
  color: var(--color-text-muted);
}

.pd-empty-t {
  display: block;
  margin-bottom: 6px;
  font-family: var(--font-family-serif);
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-main);
}

.pd-empty-d {
  font-size: var(--font-size-caption);
  line-height: 1.8;
  color: var(--color-text-muted);
}

.pd-gate-acts {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
}

/* ═══ 头图占位（深色底，不破图） ═══ */
.pd-hd-ph {
  position: relative;
  width: 100%;
  height: 100%;
  background: var(--color-panel-deep);
}

.pd-hd-ph-in {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--color-on-media-faint);
  font-size: var(--font-size-caption);
}

/* 正文两列的水平内边距由原语 .v4-immers-body 统一提供（style.css），
   本视图不再重复声明，避免同一职责出现两个 owner。 */

.pd-col {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}

.pd-side {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
  min-width: 0;
}

.pd-kvs {
  display: flex;
  flex-direction: column;
  gap: 11px;
}

.pd-ellip {
  max-width: 58%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pd-pre {
  white-space: pre-wrap;
  word-break: break-word;
}

/* ═══ 作者行与标签 ═══ */
.pd-author {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pd-author-txt {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.pd-author-txt > b {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-main);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pd-author-txt > span {
  font-size: var(--font-size-meta);
  color: var(--color-text-faint);
}

.pd-follow {
  flex: 0 0 auto;
  height: 36px;
  padding: 0 16px;
  font-size: var(--font-size-caption);
  border-radius: var(--radius-full);
}

.pd-chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
  margin-top: var(--spacing-md);
}

.pd-chip {
  cursor: default;
  pointer-events: none;
  height: 28px;
  padding: 0 12px;
}

/* ═══ 互动行 / 未登录提示 ═══ */
.pd-interact {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: var(--spacing-md);
}

.pd-like.is-on {
  color: var(--color-accent-text);
  border-color: var(--color-border-gold);
  background: var(--color-accent-soft);
}

.pd-login-tip {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: var(--spacing-md);
}

.pd-login-tip > b {
  display: inline;
  margin-bottom: 0;
}

.pd-model-link {
  margin-top: var(--spacing-md);
}

@media (max-width: 900px) {
  .pd-skel-hd { height: 160px; }
  .pd-ellip { max-width: 62%; }
}
</style>
