<template>
  <div class="detail-page animate-fade-in" v-if="post">
    <!-- 返回栏 -->
    <div class="back-bar">
      <button class="back-btn" @click="$router.back()">
        <el-icon :size="18"><ArrowLeft /></el-icon>
        返回游廊
      </button>
    </div>

    <!-- 头图 -->
    <div class="hero-section">
      <el-image
        v-if="post.preview2dPath"
        :src="post.preview2dPath"
        fit="cover"
        class="hero-image"
      >
        <template #error>
          <div class="hero-placeholder">
            <el-icon size="64"><PictureFilled /></el-icon>
          </div>
        </template>
      </el-image>
      <div v-else class="hero-placeholder">
        <el-icon size="64"><PictureFilled /></el-icon>
      </div>
      <div class="hero-gradient"></div>
    </div>

    <!-- 正文卡片 -->
    <div class="content-card glass-card">
      <!-- 标签 -->
      <div class="tag-row" v-if="post.tags">
        <span v-for="tag in parseTags(post.tags)" :key="tag" class="detail-tag">{{ tag }}</span>
      </div>

      <h1 class="post-title">{{ post.title }}</h1>

      <!-- 作者行 -->
      <div class="author-row">
        <div class="author-info">
          <el-avatar :size="44" :icon="UserFilled" />
          <div>
            <span class="author-name">{{ post.authorNickname || '匿名' }}</span>
            <span class="author-date">{{ post.createdAt }} 发布</span>
          </div>
        </div>
        <el-button round @click="handleFollow">
          {{ post.followedByMe ? '已关注' : '关注作者' }}
        </el-button>
      </div>

      <!-- 正文 -->
      <div class="post-body">{{ post.content }}</div>

      <!-- 互动按钮 -->
      <div class="interact-row">
        <button class="interact-btn" :class="{ active: post.likedByMe }" @click="handleLike">
          <el-icon :size="20"><StarFilled v-if="post.likedByMe" /><Star v-else /></el-icon>
          <span>{{ post.likeCount || 0 }} 赞赏</span>
        </button>
        <button class="interact-btn">
          <el-icon :size="20"><ChatLineSquare /></el-icon>
          <span>{{ post.commentCount || 0 }} 探讨</span>
        </button>
      </div>
    </div>

    <!-- 评论区 -->
    <div class="comments-card glass-card">
      <h3 class="comments-title">文化探讨 ({{ post.commentCount || 0 }})</h3>

      <div class="comment-input-wrap" v-if="userStore.isLoggedIn">
        <el-avatar :size="36" :icon="UserFilled" />
        <div class="comment-field">
          <el-input
            v-model="commentText"
            type="textarea"
            :rows="3"
            placeholder="在此抒发您的见解与共鸣..."
            resize="none"
          />
          <el-button type="primary" size="small" class="comment-submit" :loading="submittingComment" @click="submitComment">
            发表
          </el-button>
        </div>
      </div>

      <div v-if="post.comments && post.comments.length" class="comments-list">
        <div v-for="c in post.comments" :key="c.id" class="comment-item">
          <el-avatar :size="32" :icon="UserFilled" />
          <div class="comment-body">
            <div class="comment-author">{{ c.nickname || '匿名' }}</div>
            <div class="comment-text">{{ c.content }}</div>
            <div class="comment-time">{{ c.createdAt }}</div>
          </div>
        </div>
      </div>

      <div v-if="!loading && (!post.comments || !post.comments.length)" class="comments-empty">
        "静水流深，等待第一缕思想的涟漪"
      </div>
    </div>
  </div>

  <div v-else-if="loading" class="detail-loading flex-center">
    <el-icon :size="48" class="loading-spin"><Loading /></el-icon>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, PictureFilled, UserFilled, Star, StarFilled, ChatLineSquare } from '@element-plus/icons-vue'
import { communityApi } from '@/api/community'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const post = ref(null)
const loading = ref(true)
const commentText = ref('')
const submittingComment = ref(false)

function parseTags(tags) {
  if (!tags) return []
  if (Array.isArray(tags)) return tags
  return tags.split(',').map(t => t.trim()).filter(Boolean)
}

async function fetchPost() {
  loading.value = true
  try {
    const res = await communityApi.getPostDetail(route.params.id)
    post.value = res.data
  } catch { /* ignore */ }
  loading.value = false
}

async function handleLike() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  const prevLiked = post.value.likedByMe
  const prevCount = post.value.likeCount || 0
  post.value.likedByMe = !post.value.likedByMe
  post.value.likeCount = prevCount + (post.value.likedByMe ? 1 : -1)
  try {
    await communityApi.toggleLike({ targetId: post.value.postId, targetType: 'POST' })
  } catch {
    post.value.likedByMe = prevLiked
    post.value.likeCount = prevCount
  }
}

async function handleFollow() {
  if (!userStore.isLoggedIn) return ElMessage.warning('请先登录')
  const prevFollowed = post.value.followedByMe
  post.value.followedByMe = !post.value.followedByMe
  try {
    await communityApi.toggleFollow(post.value.authorId)
    ElMessage.success(post.value.followedByMe ? '已关注' : '已取消关注')
  } catch {
    post.value.followedByMe = prevFollowed
  }
}

async function submitComment() {
  if (!commentText.value.trim()) return
  submittingComment.value = true
  try {
    const res = await communityApi.addComment(post.value.postId, { content: commentText.value })
    ElMessage.success('评论成功')
    if (!post.value.comments) post.value.comments = []
    post.value.comments.push(res.data)
    commentText.value = ''
    post.value.commentCount = (post.value.commentCount || 0) + 1
  } catch { /* ignore */ }
  finally {
    submittingComment.value = false
  }
}

onMounted(() => {
  fetchPost()
})
</script>

<style scoped>
.detail-page {
  height: 100%;
  overflow-y: auto;
  position: relative;
}

.detail-page::-webkit-scrollbar { width: 6px; }
.detail-page::-webkit-scrollbar-track { background: transparent; }
.detail-page::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.1); border-radius: 3px; }

.detail-loading {
  height: 100%;
  color: var(--color-text-muted);
}

/* ═══ 返回栏 ═══ */
.back-bar {
  position: sticky;
  top: 0;
  z-index: 30;
  padding: 16px 24px;
  background: linear-gradient(to bottom, rgba(0,0,0,0.4), transparent);
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  background: rgba(255,255,255,0.2);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: var(--radius-full);
  color: #FFF;
  font-size: 14px;
  cursor: pointer;
  transition: background var(--transition-fast);
}

.back-btn:hover {
  background: rgba(255,255,255,0.25);
}

/* ═══ 头图 ═══ */
.hero-section {
  width: 100%;
  height: 50vh;
  min-height: 360px;
  margin-top: -64px;
  position: relative;
}

.hero-image {
  width: 100%;
  height: 100%;
}

.hero-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
}

.hero-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, var(--color-bg-base) 0%, transparent 40%);
}

/* ═══ 正文卡片 ═══ */
.content-card {
  max-width: 760px;
  margin: -80px auto 0;
  padding: var(--spacing-xl);
  position: relative;
  z-index: 10;
}

.tag-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: var(--spacing-lg);
}

.detail-tag {
  padding: 4px 14px;
  background: var(--color-accent-soft);
  color: var(--color-accent);
  border: 1px solid rgba(217, 119, 6, 0.2);
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 500;
}

.post-title {
  font-family: var(--font-family-serif);
  font-size: 32px;
  font-weight: 600;
  color: var(--color-text-main);
  line-height: 1.3;
  margin-bottom: var(--spacing-lg);
}

/* ═══ 作者行 ═══ */
.author-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--spacing-lg) 0;
  border-top: 1px solid var(--color-border);
  border-bottom: 1px solid var(--color-border);
  margin-bottom: var(--spacing-lg);
}

.author-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-name {
  display: block;
  font-weight: 500;
  color: var(--color-text-main);
}

.author-date {
  display: block;
  font-size: 13px;
  color: var(--color-text-muted);
}

/* ═══ 正文 ═══ */
.post-body {
  font-size: 16px;
  line-height: 2;
  color: var(--color-text-main);
  white-space: pre-wrap;
  margin-bottom: var(--spacing-xl);
}

/* ═══ 互动按钮 ═══ */
.interact-row {
  display: flex;
  justify-content: center;
  gap: var(--spacing-xl);
  padding-top: var(--spacing-lg);
  border-top: 1px solid var(--color-border);
}

.interact-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 28px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  background: var(--color-bg-subtle);
  color: var(--color-text-sub);
  font-size: 15px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.interact-btn:hover {
  color: var(--color-accent);
  border-color: var(--color-accent);
}

.interact-btn.active {
  color: var(--color-rose);
  border-color: var(--color-rose);
  background: var(--color-rose-soft);
}

/* ═══ 评论区 ═══ */
.comments-card {
  max-width: 760px;
  margin: var(--spacing-lg) auto 40px;
  padding: var(--spacing-xl);
}

.comments-title {
  font-family: var(--font-family-serif);
  font-size: 20px;
  color: var(--color-text-main);
  margin-bottom: var(--spacing-lg);
}

.comment-input-wrap {
  display: flex;
  gap: 12px;
  margin-bottom: var(--spacing-lg);
}

.comment-field {
  flex: 1;
  position: relative;
}

.comment-submit {
  position: absolute;
  bottom: 8px;
  right: 8px;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: var(--spacing-md) 0;
  border-bottom: 1px solid var(--color-border);
}

.comment-author {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-main);
  margin-bottom: 4px;
}

.comment-text {
  font-size: 14px;
  color: var(--color-text-main);
  line-height: 1.6;
}

.comment-time {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 4px;
}

.comments-empty {
  text-align: center;
  padding: 40px;
  color: var(--color-text-muted);
  font-family: var(--font-family-serif);
  font-size: 15px;
}

@media (max-width: 768px) {
  .content-card,
  .comments-card {
    margin-left: var(--spacing-md);
    margin-right: var(--spacing-md);
    padding: var(--spacing-lg);
  }

  .post-title {
    font-size: 24px;
  }

  .hero-section {
    height: 35vh;
    min-height: 240px;
  }
}
</style>
