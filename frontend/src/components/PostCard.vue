<template>
  <div class="feed-card" @click="router.push(`/community/post/${post.postId}`)">
    <!-- 图片区 -->
    <div class="card-cover-wrap">
      <!-- P3-D：改用原生 loading="lazy" + decoding="async"。
           el-image 的 lazy 依赖滚动容器判定，而真实滚动容器是 .view-host 而非 window；
           原生属性由浏览器直接调度，也省掉每图一个 IntersectionObserver 的开销。 -->
      <img
        v-if="post.preview2dPath && !coverBroken"
        :src="post.preview2dPath"
        :alt="post.title || '档案封面'"
        class="card-cover"
        width="1536"
        height="1152"
        loading="lazy"
        decoding="async"
        @error="coverBroken = true"
      />
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
          <button
            class="action-btn"
            :class="{ liked: post.likedByMe }"
            :aria-pressed="post.likedByMe ? 'true' : 'false'"
            :aria-label="`赞赏，当前 ${post.likeCount || 0}`"
            @click.stop="handleLikeClick"
          >
            <el-icon :size="16"><StarFilled v-if="post.likedByMe" /><Star v-else /></el-icon>
            <span>{{ post.likeCount || 0 }}</span>
          </button>
          <button class="action-btn" :aria-label="`探讨，当前 ${post.commentCount || 0}`" @click.stop>
            <el-icon :size="16"><ChatLineSquare /></el-icon>
            <span>{{ post.commentCount || 0 }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
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

/* 封面 404 时回退为占位块，避免破图 */
const coverBroken = ref(false)
watch(() => props.post.preview2dPath, () => { coverBroken.value = false })

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
/* ═══ 社区信息流卡片（V4 令牌化）═══ */
.feed-card {
  border-radius: var(--radius-2xl);
  overflow: hidden;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-card);
  transition: transform var(--transition-fast), box-shadow var(--transition-fast),
    border-color var(--transition-fast);
  cursor: pointer;
  display: flex;
  flex-direction: column;
}

.feed-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-card-hover);
  border-color: var(--color-border-gold);
}

/* ═══ 封面（P0-D3：显式 4:3 比例，消除图片加载期 reflow / CLS）═══ */
.card-cover-wrap {
  position: relative;
  width: 100%;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  background: var(--color-bg-subtle);
  contain: layout paint;
}

.card-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.card-cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
}

/* hover 反馈：金色渐变遮罩淡入（不重采样位图；原 scale 会对大图逐帧重采样） */
.card-cover-wrap::after {
  content: '';
  position: absolute;
  inset: 0;
  opacity: 0;
  transition: opacity var(--transition-fast);
  background: linear-gradient(to top, var(--color-accent-soft), transparent 62%);
  pointer-events: none;
  z-index: 1;
}

.feed-card:hover .card-cover-wrap::after {
  opacity: 1;
}

/* ═══ 卡片标签（P0-D5：实底 pill，移除 backdrop-filter）═══ */
.card-tags {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  gap: 6px;
  z-index: 2;
  flex-wrap: wrap;
  max-width: calc(100% - 24px);
}

.card-tag {
  padding: 4px 12px;
  background: var(--color-surface-float);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  font-size: 12px;
  color: var(--color-text-main);
  font-weight: 500;
}

/* ═══ 信息区 ═══ */
.card-body {
  padding: 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.card-title {
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
  color: var(--color-accent-text);
}

.card-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  color: var(--color-text-sub);
  margin-top: auto;
  padding-top: var(--spacing-sm);
  border-top: 1px solid var(--color-border);
}

.author {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  min-width: 0;
}

.card-actions {
  display: flex;
  gap: var(--spacing-md);
  align-items: center;
  flex: 0 0 auto;
}

.action-btn {
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

.action-btn:hover,
.action-btn.liked {
  color: var(--color-rose-text);
}

/* ═══ 点赞弹跳 ═══ */
@keyframes like-pop {
  0% { transform: scale(1); }
  50% { transform: scale(1.3); }
  100% { transform: scale(1); }
}

.action-btn.liked .el-icon {
  animation: like-pop 0.2s ease;
}
</style>
