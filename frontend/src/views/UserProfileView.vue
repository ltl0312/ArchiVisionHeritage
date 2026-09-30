<template>
  <div class="view-shell">
    <!-- ═══════════ ① 身份首屏条：只渲染 GET /users/me 真实返回的字段 ═══════════
         后端没有等级 / 认证 / 成就体系，因此这里不出现任何「Lv.x」「XX 认证」字样。 -->
    <section class="v4-card me-head" aria-label="我的账号信息">
      <el-skeleton v-if="profileLoading" class="me-head-skel" animated :rows="2" />

      <div v-else-if="profileError" class="me-error" role="alert">
        <h3>账号信息加载失败</h3>
        <p>{{ profileError.message }}</p>
        <div class="me-error-acts">
          <button type="button" class="v4-btn v4-btn--outline v4-btn--sm" @click="loadProfile">
            重新加载
          </button>
          <router-link v-if="profileError.needLogin" class="v4-btn v4-btn--gold v4-btn--sm" to="/login">
            去登录
          </router-link>
        </div>
      </div>

      <template v-else>
        <button type="button" class="me-avatar" aria-label="更换头像" @click="openAvatarDialog">
          <el-avatar :size="64" :src="profile.avatarUrl" :icon="UserFilled" />
          <span class="me-avatar-mask" aria-hidden="true">
            <el-icon :size="16"><Camera /></el-icon>
          </span>
        </button>

        <div class="me-id">
          <h1>{{ displayName }}</h1>
          <p class="me-username">@{{ username }}</p>
          <p class="me-bio">{{ profile.bio || '还没有填写文化签名 —— 一句话写下你的古建情怀。' }}</p>
          <div class="v4-headstats">
            <span class="v4-pill" :class="isAdmin ? 'v4-pill--gold' : 'v4-pill--mut'">{{ roleLabel }}</span>
            <span v-if="joinedLabel" class="v4-pill v4-pill--mut v4-pill--bare">
              入册于 {{ joinedLabel }}
            </span>
            <span
              v-if="postsTotal !== null"
              class="v4-pill v4-pill--jade v4-pill--bare"
              title="含待审核与已驳回的档案"
            >
              档案 {{ postsTotal }} 件
            </span>
            <span v-if="likesTotal !== null" class="v4-pill v4-pill--mut v4-pill--bare">
              收藏 {{ likesTotal }} 件
            </span>
          </div>
        </div>

        <div class="me-acts">
          <button type="button" class="v4-btn v4-btn--ghost v4-btn--sm" @click="openEditDialog">
            <el-icon :size="14"><Edit /></el-icon>
            编辑资料
          </button>
          <button type="button" class="v4-btn v4-btn--ghost v4-btn--sm" @click="openPasswordDialog">
            <el-icon :size="14"><Lock /></el-icon>
            修改密码
          </button>
        </div>
      </template>
    </section>

    <!-- ═══════════ ② 档案与收藏 ═══════════ -->
    <div class="v4-sechead">
      <h2>档案总览</h2>
      <span class="k">仅本人可见 · 含待审核与已驳回</span>
    </div>

    <div class="v4-card me-body">
      <div class="v4-toolrow me-toolrow">
        <div class="v4-seg" role="tablist" aria-label="档案分类" @keydown="onTabKeydown">
          <button
            v-for="t in TABS"
            :id="`tab-${t.key}`"
            :key="t.key"
            type="button"
            role="tab"
            :aria-selected="activeTab === t.key"
            :aria-controls="`panel-${t.key}`"
            :tabindex="activeTab === t.key ? 0 : -1"
            :class="{ 'is-on': activeTab === t.key }"
            @click="activeTab = t.key"
          >
            {{ t.label }}
          </button>
        </div>
        <span class="me-toolhint">{{ activeTab === 'posts' ? postsHint : likesHint }}</span>
      </div>

      <!-- ─────────── 面板一：我的档案（GET /users/me/posts） ─────────── -->
      <div v-show="activeTab === 'posts'" id="panel-posts" role="tabpanel" aria-labelledby="tab-posts">
        <!-- 状态筛选：基于后端真实返回的 status 字段，仅作用于当前已加载页 -->
        <div class="v4-chips me-chips" role="group" aria-label="按审核状态筛选我的档案">
          <button
            v-for="f in STATUS_FILTERS"
            :key="f.key"
            type="button"
            class="v4-chip"
            :class="{ 'is-active': statusFilter === f.key }"
            :aria-pressed="statusFilter === f.key"
            @click="statusFilter = f.key"
          >
            {{ f.label }}
          </button>
        </div>

        <p v-if="filterActive && !postsLoading && !postsError && myPosts.length" class="me-filternote">
          在当前已加载的 {{ myPosts.length }} 条中筛选（第 {{ postsPage }} 页）· 命中
          {{ visiblePosts.length }} 条
        </p>

        <!-- 加载中 -->
        <div v-if="postsLoading" class="v4-cards3" aria-busy="true" aria-label="正在加载我的档案">
          <div v-for="n in 3" :key="n" class="v4-card me-skel">
            <el-skeleton animated>
              <template #template>
                <el-skeleton-item variant="image" class="me-skel-img" />
                <div class="me-skel-txt">
                  <el-skeleton-item variant="h3" style="width: 72%" />
                  <el-skeleton-item variant="text" style="width: 52%" />
                </div>
              </template>
            </el-skeleton>
          </div>
        </div>

        <!-- 错误（含 401 未登录/登录失效） -->
        <div v-else-if="postsError" class="v4-card me-error" role="alert">
          <h3>我的档案加载失败</h3>
          <p>{{ postsError.message }}</p>
          <div class="me-error-acts">
            <button type="button" class="v4-btn v4-btn--outline v4-btn--sm" @click="loadPosts(postsPage)">
              重试
            </button>
            <router-link v-if="postsError.needLogin" class="v4-btn v4-btn--gold v4-btn--sm" to="/login">
              去登录
            </router-link>
          </div>
        </div>

        <!-- 空：真的一条档案都没有 -->
        <div v-else-if="!myPosts.length" class="v4-empty">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M3 9.5L12 3.5l9 6" />
            <path d="M5.5 10.5v9h13v-9" />
            <path d="M9.5 19.5v-5h5v5" />
          </svg>
          <p>还没有档案 · 去一键幻筑营造第一件</p>
          <p class="me-empty-sub">
            发布后的档案先进入「待审核」，它的状态会一直在这里显示 —— 不必再等审核结果的通知。
          </p>
          <router-link class="v4-btn v4-btn--gold v4-btn--sm me-empty-cta" to="/huanzhu">
            去一键幻筑
          </router-link>
        </div>

        <!-- 空：当前页没有匹配筛选条件的档案（与「一条都没有」区分） -->
        <div v-else-if="!visiblePosts.length" class="v4-empty">
          <p>当前页没有「{{ activeFilterLabel }}」的档案</p>
          <p class="me-empty-sub">
            筛选只作用于当前已加载的这一页；翻页或清除筛选即可看到其他档案。
          </p>
          <button
            type="button"
            class="v4-btn v4-btn--outline v4-btn--sm me-empty-cta"
            @click="statusFilter = 'ALL'"
          >
            清除筛选
          </button>
        </div>

        <!-- 有数据 -->
        <template v-else>
          <div class="v4-cards3">
            <ArchiveCard
              v-for="post in visiblePosts"
              :key="post.postId"
              :to="`/archive/${post.postId}`"
              :title="post.title || '未命名档案'"
              :image="post.preview2dPath"
              :meta="post.meta"
              :tags="post.tags"
              :badge="post.badge"
              :badge-tone="post.badgeTone"
            />
          </div>
          <div v-if="postsTotal > PAGE_SIZE" class="me-pager">
            <el-pagination
              background
              layout="prev, pager, next"
              :total="postsTotal"
              :page-size="PAGE_SIZE"
              :current-page="postsPage"
              @current-change="loadPosts"
            />
          </div>
        </template>
      </div>

      <!-- ─────────── 面板二：我的收藏（GET /users/me/likes） ─────────── -->
      <div v-show="activeTab === 'likes'" id="panel-likes" role="tabpanel" aria-labelledby="tab-likes">
        <!-- 加载中 -->
        <div v-if="likesLoading" class="v4-cards3" aria-busy="true" aria-label="正在加载我的收藏">
          <div v-for="n in 3" :key="n" class="v4-card me-skel">
            <el-skeleton animated>
              <template #template>
                <el-skeleton-item variant="image" class="me-skel-img" />
                <div class="me-skel-txt">
                  <el-skeleton-item variant="h3" style="width: 72%" />
                  <el-skeleton-item variant="text" style="width: 52%" />
                </div>
              </template>
            </el-skeleton>
          </div>
        </div>

        <!-- 错误 -->
        <div v-else-if="likesError" class="v4-card me-error" role="alert">
          <h3>我的收藏加载失败</h3>
          <p>{{ likesError.message }}</p>
          <div class="me-error-acts">
            <button type="button" class="v4-btn v4-btn--outline v4-btn--sm" @click="loadLikes(likesPage)">
              重试
            </button>
            <router-link v-if="likesError.needLogin" class="v4-btn v4-btn--gold v4-btn--sm" to="/login">
              去登录
            </router-link>
          </div>
        </div>

        <!-- 空 -->
        <div v-else-if="!likedPosts.length" class="v4-empty">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M12 20.2C9.6 18.4 4.5 14.6 4.5 10.8A4.3 4.3 0 0 1 12 7.6a4.3 4.3 0 0 1 7.5 3.2c0 3.8-5.1 7.6-7.5 9.4Z" />
          </svg>
          <p>还没有收藏 · 去匠人社区看看</p>
          <p class="me-empty-sub">在社区里为喜欢的档案点亮爱心，它就会出现在这里。</p>
          <router-link class="v4-btn v4-btn--gold v4-btn--sm me-empty-cta" to="/community">
            去匠人社区
          </router-link>
        </div>

        <!-- 有数据 -->
        <template v-else>
          <div class="v4-cards3">
            <ArchiveCard
              v-for="post in likedPosts"
              :key="post.postId"
              :to="`/archive/${post.postId}`"
              :title="post.title || '未命名档案'"
              :image="post.preview2dPath"
              :meta="post.meta"
              :tags="post.tags"
              :badge="post.badge"
              :badge-tone="post.badgeTone"
            />
          </div>
          <div v-if="likesTotal > PAGE_SIZE" class="me-pager">
            <el-pagination
              background
              layout="prev, pager, next"
              :total="likesTotal"
              :page-size="PAGE_SIZE"
              :current-page="likesPage"
              @current-change="loadLikes"
            />
          </div>
        </template>
      </div>
    </div>

    <!-- ═══════════ ③ 头像上传弹窗（PUT /users/me { avatarUrl }） ═══════════ -->
    <el-dialog
      v-model="showAvatarDialog"
      title="更换头像"
      width="min(420px, 92vw)"
      :close-on-click-modal="false"
    >
      <div class="me-dialog-avatar">
        <el-avatar :size="120" :src="avatarForm.avatarUrl" :icon="UserFilled" />
      </div>
      <ImageUploader
        v-model="avatarForm.avatarUrl"
        placeholder="上传新头像"
        :show-url-input="true"
        sub-dir="avatars"
      />
      <template #footer>
        <button type="button" class="v4-btn v4-btn--ghost v4-btn--sm" @click="showAvatarDialog = false">
          取消
        </button>
        <button
          type="button"
          class="v4-btn v4-btn--gold v4-btn--sm"
          :disabled="savingAvatar"
          @click="saveAvatar"
        >
          {{ savingAvatar ? '保存中…' : '保存头像' }}
        </button>
      </template>
    </el-dialog>

    <!-- ═══════════ ④ 编辑资料弹窗（PUT /users/me { nickname, bio }） ═══════════ -->
    <el-dialog
      v-model="showEditDialog"
      title="编辑个人资料"
      width="min(520px, 92vw)"
      :close-on-click-modal="false"
    >
      <el-form :model="editForm" label-position="top">
        <el-form-item label="昵称">
          <el-input
            v-model="editForm.nickname"
            placeholder="设置您的显示名称"
            maxlength="64"
            show-word-limit
            :prefix-icon="UserFilled"
          />
        </el-form-item>
        <el-form-item label="文化签名">
          <el-input
            v-model="editForm.bio"
            type="textarea"
            :rows="4"
            placeholder="一句话介绍你的古建情怀..."
            maxlength="255"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <button type="button" class="v4-btn v4-btn--ghost v4-btn--sm" @click="showEditDialog = false">
          取消
        </button>
        <button
          type="button"
          class="v4-btn v4-btn--gold v4-btn--sm"
          :disabled="savingProfile"
          @click="saveProfile"
        >
          {{ savingProfile ? '保存中…' : '保存修改' }}
        </button>
      </template>
    </el-dialog>

    <!-- ═══════════ ⑤ 修改密码弹窗（PUT /users/me/password） ═══════════ -->
    <el-dialog
      v-model="showPasswordDialog"
      title="修改密码"
      width="min(420px, 92vw)"
      :close-on-click-modal="false"
    >
      <el-form :model="passwordForm" label-position="top">
        <el-form-item label="当前密码">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            placeholder="请输入当前密码"
            show-password
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="至少6位"
            show-password
            :prefix-icon="Key"
          />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="再次输入新密码"
            show-password
            :prefix-icon="Key"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <button type="button" class="v4-btn v4-btn--ghost v4-btn--sm" @click="showPasswordDialog = false">
          取消
        </button>
        <button
          type="button"
          class="v4-btn v4-btn--gold v4-btn--sm"
          :disabled="changingPwd"
          @click="changePassword"
        >
          {{ changingPwd ? '提交中…' : '修改密码' }}
        </button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { Camera, Edit, Key, Lock, UserFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import ArchiveCard from '@/components/ArchiveCard.vue'
import ImageUploader from '@/components/ImageUploader.vue'
import { authApi } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { recordsFallback } from '@/utils/pagination'
import { parseTags } from '@/utils/format'

/**
 * 我的档案（路由 /archive · 计划书决策 6：改造本视图，不另建列表页）
 *
 * ⚠️ 与计划书原稿的一处既定偏离（已由主协调者裁定）：
 *   原稿写「Tab = 我的解析 / 我的幻筑 / 我的收藏」，但后端契约不支持这个三分 ——
 *     · GET /users/me/posts 返回 PostBriefResponse，字段中没有 modelAssetId，
 *       无法区分一条档案是「实景解析」还是「AI 幻筑」；
 *     · 后端也没有任何「按用户持久化的解析记录」接口（analysis_demo 是全局演示数据）。
 *   故本页实现为 2 个 Tab（我的档案 / 我的收藏）+ 一组基于真实 status 字段的状态筛选 chip。
 *   附带解决一个真实产品缺口：改造前，用户发帖后（PENDING）无处查看自己的待审 / 被驳回档案。
 *
 * 数据源（全部为真实接口，未编造任何字段）：
 *   GET /api/v1/users/me         → { id, username, nickname, avatarUrl, bio, role, createdAt }
 *   GET /api/v1/users/me/posts   → Page<PostBriefResponse>
 *   GET /api/v1/users/me/likes   → Page<PostBriefResponse>
 */

const userStore = useUserStore()

const PAGE_SIZE = 12

const TABS = [
  { key: 'posts', label: '我的档案' },
  { key: 'likes', label: '我的收藏' }
]

/* 状态筛选：取值与后端 Post.status 一一对应（APPROVED / PENDING / REJECTED） */
const STATUS_FILTERS = [
  { key: 'ALL', label: '全部' },
  { key: 'APPROVED', label: '已发布' },
  { key: 'PENDING', label: '待审核' },
  { key: 'REJECTED', label: '已驳回' }
]

const activeTab = ref('posts')
const statusFilter = ref('ALL')

/* ── 账号信息 ── */
const profile = ref({})
const profileLoading = ref(true)
const profileError = ref(null)

/* ── 我的档案 ── */
const myPosts = ref([])
const postsTotal = ref(null)
const postsPage = ref(1)
const postsLoading = ref(true)
const postsError = ref(null)

/* ── 我的收藏 ── */
const likedPosts = ref([])
const likesTotal = ref(null)
const likesPage = ref(1)
const likesLoading = ref(true)
const likesError = ref(null)

/* ── 弹窗与表单 ── */
const showAvatarDialog = ref(false)
const showEditDialog = ref(false)
const showPasswordDialog = ref(false)
const avatarForm = ref({ avatarUrl: '' })
const editForm = ref({ nickname: '', bio: '' })
const passwordForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const savingAvatar = ref(false)
const savingProfile = ref(false)
const changingPwd = ref(false)

/* ═══════════ 派生展示值（全部来自接口真实字段） ═══════════ */

const displayName = computed(
  () => profile.value.nickname || profile.value.username || userStore.username || '未命名匠人'
)
const username = computed(() => profile.value.username || userStore.username || 'unknown')
const isAdmin = computed(() => profile.value.role === 'ADMIN')
const roleLabel = computed(() => (isAdmin.value ? '管理员' : '注册用户'))
const joinedLabel = computed(() => formatDate(profile.value.createdAt))

const postsHint = computed(() =>
  typeof postsTotal.value === 'number' ? `共 ${postsTotal.value} 件档案` : ''
)
const likesHint = computed(() =>
  typeof likesTotal.value === 'number' ? `共 ${likesTotal.value} 件收藏` : ''
)

const filterActive = computed(() => statusFilter.value !== 'ALL')
const activeFilterLabel = computed(
  () => STATUS_FILTERS.find(f => f.key === statusFilter.value)?.label || ''
)

/* 筛选只作用于「当前已加载的这一页」—— 文案如实标注，不假装是全量筛选 */
const visiblePosts = computed(() =>
  filterActive.value ? myPosts.value.filter(p => p.status === statusFilter.value) : myPosts.value
)

/* ═══════════ 格式化（不引入后端没有的字段） ═══════════ */

function formatDate(raw) {
  if (!raw) return ''
  return String(raw).replace('T', ' ').slice(0, 10)
}

/* 角标：待审核 = gold，已驳回 = red，已发布不显示（与主协调者的裁定一致） */
function statusBadge(status) {
  if (status === 'PENDING') return '待审核'
  if (status === 'REJECTED') return '已驳回'
  return ''
}

function buildMeta(post, withAuthor = false) {
  return [
    withAuthor ? post.authorNickname || '匿名匠人' : null,
    `♥ ${post.likeCount ?? 0}`,
    `评论 ${post.commentCount ?? 0}`,
    formatDate(post.createdAt)
  ]
    .filter(Boolean)
    .join(' ｜ ')
}

function decorate(post, withAuthor = false) {
  return {
    ...post,
    tags: parseTags(post.tags),
    badge: statusBadge(post.status),
    badgeTone: post.status === 'REJECTED' ? 'red' : 'gold',
    meta: buildMeta(post, withAuthor)
  }
}

/* 统一错误态：区分「登录失效」与「普通失败」，前者给出 /login 入口 */
function toErrorState(e) {
  const status = e?.response?.status
  if (status === 401) {
    return { message: '登录状态已失效，请重新登录后查看我的档案。', needLogin: true }
  }
  if (status === 403) {
    return { message: '当前账号没有权限查看这份数据。', needLogin: true }
  }
  return {
    message: e?.response?.data?.message || e?.message || '网络异常，请检查后端服务是否已启动。',
    needLogin: false
  }
}

/* ═══════════ 数据加载 ═══════════ */

async function loadProfile() {
  profileLoading.value = true
  profileError.value = null
  try {
    const res = await authApi.getCurrentUser()
    profile.value = res?.data || {}
  } catch (e) {
    profile.value = {}
    profileError.value = toErrorState(e)
  } finally {
    profileLoading.value = false
  }
}

async function loadPosts(page = 1) {
  postsLoading.value = true
  postsError.value = null
  try {
    const res = await authApi.getMyPosts(page, PAGE_SIZE)
    const data = res?.data
    const records = recordsFallback(data, [])
    myPosts.value = records.map(p => decorate(p))
    postsTotal.value = typeof data?.total === 'number' ? data.total : records.length
    postsPage.value = typeof data?.current === 'number' ? data.current : page
  } catch (e) {
    myPosts.value = []
    postsTotal.value = null
    postsError.value = toErrorState(e)
  } finally {
    postsLoading.value = false
  }
}

async function loadLikes(page = 1) {
  likesLoading.value = true
  likesError.value = null
  try {
    const res = await authApi.getMyLikes(page, PAGE_SIZE)
    const data = res?.data
    const records = recordsFallback(data, [])
    likedPosts.value = records.map(p => decorate(p, true))
    likesTotal.value = typeof data?.total === 'number' ? data.total : records.length
    likesPage.value = typeof data?.current === 'number' ? data.current : page
  } catch (e) {
    likedPosts.value = []
    likesTotal.value = null
    likesError.value = toErrorState(e)
  } finally {
    likesLoading.value = false
  }
}

/* ═══════════ Tab 键盘操作（role="tablist" 的配套：左右方向键 / Home / End） ═══════════ */

function onTabKeydown(e) {
  const keys = TABS.map(t => t.key)
  const i = keys.indexOf(activeTab.value)
  let next = null
  if (e.key === 'ArrowRight') next = keys[(i + 1) % keys.length]
  else if (e.key === 'ArrowLeft') next = keys[(i - 1 + keys.length) % keys.length]
  else if (e.key === 'Home') next = keys[0]
  else if (e.key === 'End') next = keys[keys.length - 1]
  if (!next) return
  e.preventDefault()
  activeTab.value = next
  nextTick(() => document.getElementById(`tab-${next}`)?.focus())
}

/* ═══════════ 弹窗动作（沿用改造前的行为，未回退） ═══════════ */

function openAvatarDialog() {
  avatarForm.value = { avatarUrl: profile.value.avatarUrl || '' }
  showAvatarDialog.value = true
}

function openEditDialog() {
  editForm.value = {
    nickname: profile.value.nickname || '',
    bio: profile.value.bio || ''
  }
  showEditDialog.value = true
}

function openPasswordDialog() {
  showPasswordDialog.value = true
}

async function saveAvatar() {
  if (!avatarForm.value.avatarUrl) {
    return ElMessage.warning('请先上传或填写头像地址')
  }
  savingAvatar.value = true
  try {
    await authApi.updateProfile({ avatarUrl: avatarForm.value.avatarUrl })
    profile.value.avatarUrl = avatarForm.value.avatarUrl
    showAvatarDialog.value = false
    ElMessage.success('头像已更新')
  } catch {
    /* 拦截器已提示 */
  } finally {
    savingAvatar.value = false
  }
}

async function saveProfile() {
  if (!editForm.value.nickname.trim()) {
    return ElMessage.warning('昵称不能为空')
  }
  savingProfile.value = true
  try {
    await authApi.updateProfile(editForm.value)
    profile.value.nickname = editForm.value.nickname
    profile.value.bio = editForm.value.bio
    showEditDialog.value = false
    ElMessage.success('资料已更新')
  } catch {
    /* 拦截器已提示 */
  } finally {
    savingProfile.value = false
  }
}

async function changePassword() {
  const { oldPassword, newPassword, confirmPassword } = passwordForm.value
  if (!oldPassword || !newPassword) {
    return ElMessage.warning('请填写完整密码信息')
  }
  if (newPassword.length < 6) {
    return ElMessage.warning('新密码至少6位')
  }
  if (newPassword !== confirmPassword) {
    return ElMessage.warning('两次输入的新密码不一致')
  }
  changingPwd.value = true
  try {
    await authApi.changePassword({ oldPassword, newPassword })
    ElMessage.success('密码修改成功')
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    showPasswordDialog.value = false
  } catch {
    /* 拦截器已提示 */
  } finally {
    changingPwd.value = false
  }
}

/* 三个请求各自独立四态：一个失败不影响另外两个 */
onMounted(() => {
  loadProfile()
  loadPosts(1)
  loadLikes(1)
})
</script>

<style scoped>
/* 视图内的 router-link 伪装成按钮时去掉下划线 */
a.v4-btn {
  text-decoration: none;
}

/* ═══════════ ① 身份首屏条 ═══════════ */
.me-head {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-lg);
}
.me-head-skel,
.me-head > .me-error {
  flex: 1;
  min-width: 0;
}

.me-avatar {
  position: relative;
  flex: 0 0 auto;
  padding: 0;
  border: none;
  background: none;
  border-radius: 50%;
  cursor: pointer;
  line-height: 0;
}
.me-avatar :deep(.el-avatar) {
  border: 2px solid var(--color-border);
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
}
.me-avatar-mask {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  border-radius: 50%;
  opacity: 0;
  color: var(--color-on-media-strong);
  background: var(--color-scrim-strong);
  transition: opacity var(--transition-fast);
}
.me-avatar:hover .me-avatar-mask,
.me-avatar:focus-visible .me-avatar-mask {
  opacity: 1;
}

.me-id {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.me-id > h1 {
  font-family: var(--font-family-serif);
  font-size: var(--font-size-heading);
  font-weight: 600;
  line-height: var(--line-height-heading);
  color: var(--color-text-main);
  overflow-wrap: anywhere;
}
.me-username {
  font-size: var(--font-size-caption);
  color: var(--color-text-muted);
  overflow-wrap: anywhere;
}
.me-bio {
  font-size: var(--font-size-caption);
  line-height: 1.75;
  color: var(--color-text-sub);
  overflow-wrap: anywhere;
}
.me-id .v4-headstats {
  margin-top: var(--spacing-sm);
}

.me-acts {
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
}

/* ═══════════ ② 档案与收藏 ═══════════ */
.me-body {
  padding: var(--spacing-lg);
}
.me-toolrow {
  align-items: center;
  margin-bottom: var(--spacing-md);
}
.me-toolhint {
  font-size: var(--font-size-meta);
  color: var(--color-text-faint);
}

/* .v4-chip 是给 <button> 用的：清掉 UA 默认底色，激活态在本作用域内重新声明
   （scoped 选择器特异度高于 style.css 的 .v4-chip.is-active，必须自带一条） */
.me-chips {
  margin-bottom: var(--spacing-md);
}
.me-chips > .v4-chip {
  background: none;
  font-family: inherit;
  line-height: inherit;
}
.me-chips > .v4-chip.is-active {
  background: var(--color-accent-soft);
}

.me-filternote {
  margin: calc(-1 * var(--spacing-sm)) 0 var(--spacing-md);
  font-size: var(--font-size-meta);
  line-height: 1.7;
  color: var(--color-text-faint);
}

/* 骨架占位尺寸与 ArchiveCard 对齐，避免加载完成时跳动 */
.me-skel {
  padding: 0;
  overflow: hidden;
}
.me-skel-img {
  width: 100%;
  height: 158px;
  display: block;
  border-radius: 0;
}
.me-skel-txt {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px;
}

/* 错误态 */
.me-error {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
}
.me-error > h3 {
  font-family: var(--font-family-serif);
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-main);
}
.me-error > p {
  font-size: var(--font-size-caption);
  line-height: 1.7;
  color: var(--color-text-muted);
  overflow-wrap: anywhere;
}
.me-error-acts {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
}

/* 空态补充说明与行动 */
.me-empty-sub {
  margin: 6px 0 18px;
  font-size: var(--font-size-caption);
  line-height: 1.75;
  color: var(--color-text-faint);
}
.me-empty-cta {
  margin-top: var(--spacing-xs);
}

/* 分页 */
.me-pager {
  display: flex;
  justify-content: center;
  margin-top: var(--spacing-lg);
}
.me-pager :deep(.el-pagination) {
  flex-wrap: wrap;
  justify-content: center;
  row-gap: var(--spacing-xs);
}

/* ═══════════ 弹窗 ═══════════ */
.me-dialog-avatar {
  display: flex;
  justify-content: center;
  margin-bottom: var(--spacing-lg);
}
.me-dialog-avatar :deep(.el-avatar) {
  border: 3px solid var(--color-border);
  box-shadow: var(--shadow-card);
}

/* ═══════════ 本视图专属窄屏微调（全局 ≤900px 已处理 .v4-cards3） ═══════════ */
@media (max-width: 900px) {
  .me-head {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-md);
  }
  .me-acts {
    width: 100%;
  }
  .me-acts > .v4-btn {
    flex: 1 1 auto;
  }
  .me-body {
    padding: var(--spacing-md);
  }
  .me-pager :deep(.el-pagination) {
    row-gap: var(--spacing-sm);
  }
}
</style>
