<template>
  <div class="feed-card" @click="router.push(`/post/${post.postId}`)">
    <!-- 图片区 -->
    <div class="card-cover-wrap">
      <el-image v-if="post.preview2dPath" lazy :src="post.preview2dPath" fit="cover" class="card-cover">
        <template #error>
          <div class="card-cover-placeholder">
            <el-icon size="48"><PictureFilled /></el-icon>
          </div>
        </template>
      </el-image>
      <div v-else class="card-cover-placeholder">
        <el-icon size="48"><PictureFilled /></el-icon>
      </div>
      <!-- 标签浮层 -->
      <div class="card-tags" v-if="post.tags">
        <span v-for="tag in parseTags(post.tags)" :key="tag" class="card-tag">{{ tag }}</span>
      </div>
    </div>

    <!-- 信息区 -->
    <div class="card-body">
      <h3 class="card-title">{{ post.title }}</h3>
      <div class="card-meta">
        <div class="author">
          <el-avatar :size="24" :icon="UserFilled" />
          <span>{{ post.authorNickname || '匿名' }}</span>
        </div>
        <div class="card-actions">
          <button class="action-btn" :class="{ liked: post.likedByMe }" @click.stop="handleLikeClick">
            <el-icon :size="16"><StarFilled v-if="post.likedByMe" /><Star v-else /></el-icon>
            <span>{{ post.likeCount || 0 }}</span>
          </button>
          <button class="action-btn" @click.stop>
            <el-icon :size="16"><ChatLineSquare /></el-icon>
            <span>{{ post.commentCount || 0 }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { PictureFilled, UserFilled, Star, StarFilled, ChatLineSquare } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { communityApi } from '@/api/community'
import { useUserStore } from '@/stores/user'
import { parseTags } from '@/utils/format'
import { useOptimisticLike } from '@/composables/useOptimisticLike'

const props = defineProps({
  post: { type: Object, required: true }
})

const router = useRouter()
const userStore = useUserStore()

const { toggle } = useOptimisticLike({
  getTarget: () => props.post,
  api: (p) => communityApi.toggleLike({ targetId: p.postId, targetType: 'POST' })
})

function handleLikeClick() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  toggle()
}
</script>

<style scoped>
/* ═══ 社区卡片（原 style.css 社区卡片块逐字搬移）═══ */
.feed-card {
  border-radius: var(--radius-2xl);
  overflow: hidden;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-card);
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
  cursor: pointer;
  display: flex;
  flex-direction: column;
}

[data-theme="dark"] .feed-card {
  border-color: rgba(255, 255, 255, 0.06);
}

.feed-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-card-hover);
}

.feed-card .card-cover {
  width: 100%;
  display: block;
  object-fit: cover;
}

.feed-card:hover .card-cover {
  transform: scale(1.03);
}

.feed-card .card-body {
  padding: 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.feed-card .card-title {
  font-family: var(--font-family-serif);
  font-size: 17px;
  font-weight: 600;
  color: var(--color-text-main);
  margin-bottom: var(--spacing-sm);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color var(--transition-fast);
}

.feed-card:hover .card-title {
  color: var(--color-accent);
}

[data-theme="dark"] .feed-card:hover .card-title {
  color: var(--color-accent-light);
}

.feed-card .card-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  color: var(--color-text-sub);
  margin-top: auto;
  padding-top: var(--spacing-sm);
  border-top: 1px solid var(--color-border);
}

.feed-card .author {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

.feed-card .card-actions {
  display: flex;
  gap: var(--spacing-md);
  align-items: center;
}

.feed-card .card-actions .action-btn {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  font-size: 13px;
  color: var(--color-text-sub);
  cursor: pointer;
  border: none;
  background: none;
  transition: color var(--transition-fast);
}

.feed-card .card-actions .action-btn:hover {
  color: var(--color-rose);
}

.feed-card .card-actions .action-btn.liked {
  color: var(--color-rose);
}

/* 卡片标签 — 毛玻璃 pill（原 style.css 卡片标签块逐字搬移） */
.card-tags {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  gap: 6px;
  z-index: 2;
  flex-wrap: wrap;
}

.card-tag {
  padding: 4px 12px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: var(--radius-full);
  font-size: 12px;
  color: var(--color-text-main);
  font-weight: 500;
}

[data-theme="dark"] .card-tag {
  background: rgba(28, 25, 23, 0.75);
  border-color: rgba(255, 255, 255, 0.08);
  color: var(--color-text-main);
}

/* ═══ 点赞弹跳（原 style.css like-pop 逐字搬移）═══ */
@keyframes like-pop {
  0% { transform: scale(1); }
  50% { transform: scale(1.3); }
  100% { transform: scale(1); }
}

.action-btn.liked .el-icon {
  animation: like-pop 0.2s ease;
}

/* ═══ 卡片封面（原 CommunityFeedView scoped 封面块逐字搬移）═══ */
.card-cover-wrap {
  position: relative;
  width: 100%;
  overflow: hidden;
}

.card-cover-wrap .el-image {
  width: 100%;
  display: block;
}

.card-cover-placeholder {
  width: 100%;
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
}
</style>
