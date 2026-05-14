<template>
  <div class="post-detail" v-loading="loading">
    <template v-if="post">
      <!-- 3D 模型沉浸展示区 -->
      <div class="model-viewer-section" v-if="post.glb3dPath">
        <model-viewer
          v-if="isMounted"
          :src="post.glb3dPath"
          :poster="post.preview2dPath"
          :camera-controls="true"
          :auto-rotate="true"
          :auto-rotate-delay="1000"
          exposure="0.8"
          shadow-intensity="1"
          style="width: 100%; height: 480px; background: #e8e4df;"
          camera-orbit="45deg 75deg auto"
        >
          <div slot="progress-bar" style="display:flex;justify-content:center;align-items:center;height:100%;color:var(--color-text-secondary)">
            正在加载3D古建模型...
          </div>
        </model-viewer>
      </div>

      <!-- 2D 封面（无3D模型时） -->
      <div v-else-if="post.preview2dPath" class="preview-section">
        <img :src="post.preview2dPath" :alt="post.title" />
      </div>

      <!-- 帖子正文 -->
      <div class="content-section">
        <h1 class="post-title">{{ post.title }}</h1>
        <div class="post-meta">
          <span class="author-info">
            <el-avatar size="28" :icon="UserFilled" />
            <span>{{ post.authorNickname }}</span>
          </span>
          <span class="time">{{ post.createdAt }}</span>
        </div>
        <div class="post-content" v-if="post.content">{{ post.content }}</div>

        <!-- 互动按钮 -->
        <div class="interaction-bar">
          <el-button
            :type="post.likedByMe ? 'danger' : 'default'"
            size="large"
            @click="handleLike"
            :icon="post.likedByMe ? StarFilled : StarFilled"
          >
            {{ post.likedByMe ? '已赞' : '点赞' }} {{ post.likeCount }}
          </el-button>
          <el-button size="large" @click="focusComment" :icon="ChatDotRound">
            评论
          </el-button>
        </div>
      </div>

      <!-- 评论区 -->
      <div class="comments-section">
        <h3>互动交流 ({{ post.comments?.length || 0 }})</h3>

        <div class="comment-input">
          <el-input
            ref="commentInputRef"
            v-model="commentContent"
            type="textarea"
            :rows="3"
            placeholder="写下您对这座古建的感受..."
            maxlength="1024"
            show-word-limit
          />
          <el-button type="primary" @click="handleAddComment" :loading="commenting" style="margin-top:8px">
            发表评论
          </el-button>
        </div>

        <div class="comment-list" v-if="post.comments?.length">
          <div v-for="c in post.comments" :key="c.id" class="comment-item">
            <div class="comment-header">
              <el-avatar size="24" :icon="UserFilled" />
              <span class="comment-nickname">{{ c.nickname }}</span>
              <span class="comment-time">{{ c.createdAt }}</span>
            </div>
            <p class="comment-body">{{ c.content }}</p>
          </div>
        </div>
        <el-empty v-else description="暂无评论，来做第一个交流的同好吧" :image-size="60" />
      </div>
    </template>

    <el-empty v-else description="帖子不存在" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { StarFilled, ChatDotRound, UserFilled } from '@element-plus/icons-vue'
import { communityApi } from '@/api/community'

const route = useRoute()
const post = ref(null)
const loading = ref(false)
const commenting = ref(false)
const commentContent = ref('')
const commentInputRef = ref(null)
const isMounted = ref(false)

onMounted(async () => {
  isMounted.value = true
  await fetchPostDetail()
})

async function fetchPostDetail() {
  loading.value = true
  try {
    const res = await communityApi.getPostDetail(route.params.id)
    post.value = res.data
  } finally {
    loading.value = false
  }
}

async function handleLike() {
  if (!post.value) return
  try {
    await communityApi.toggleLike({ targetId: post.value.postId, targetType: 'POST' })
    post.value.likedByMe = !post.value.likedByMe
    post.value.likeCount += post.value.likedByMe ? 1 : -1
  } catch { /* ignore */ }
}

async function handleAddComment() {
  if (!commentContent.value.trim()) return ElMessage.warning('请输入评论内容')
  commenting.value = true
  try {
    await communityApi.addComment(route.params.id, { content: commentContent.value })
    ElMessage.success('评论发表成功')
    commentContent.value = ''
    await fetchPostDetail() // 刷新评论列表
  } finally {
    commenting.value = false
  }
}

function focusComment() {
  commentInputRef.value?.focus()
}
</script>

<style scoped>
.post-detail {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px;
}

.model-viewer-section {
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: 0 4px 24px rgba(0,0,0,0.12);
  margin-bottom: 24px;
}

.preview-section img {
  width: 100%;
  border-radius: var(--radius-lg);
}

.content-section {
  background: white;
  border-radius: var(--radius-lg);
  padding: 24px;
  margin-bottom: 24px;
}

.post-title {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 12px;
}

.post-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 16px;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.post-content {
  font-size: 15px;
  line-height: 1.8;
  color: var(--color-text-primary);
  margin-bottom: 20px;
}

.interaction-bar {
  display: flex;
  gap: 12px;
}

.comments-section {
  background: white;
  border-radius: var(--radius-lg);
  padding: 24px;
}

.comments-section h3 {
  font-size: 18px;
  margin-bottom: 16px;
}

.comment-input {
  margin-bottom: 24px;
}

.comment-item {
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.comment-item:last-child { border-bottom: none; }

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.comment-nickname {
  font-weight: 600;
  font-size: 14px;
}

.comment-time {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-left: auto;
}

.comment-body {
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-text-primary);
  margin-left: 32px;
}
</style>
