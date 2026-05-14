<template>
  <div class="community-feed">
    <!-- 文化标语横幅 -->
    <div class="culture-banner">
      <h2 class="banner-title">以技术为舟，载文化远航</h2>
      <p class="banner-sub">每一块瓦当、每一组斗栱，在数字世界里重获新生</p>
      <el-button type="primary" size="large" @click="$router.push('/huanzhu')">
        <el-icon><MagicStick /></el-icon>
        一键幻筑，创造你的古建
      </el-button>
    </div>

    <!-- 帖子瀑布流 -->
    <div class="waterfall-grid" v-loading="loading">
      <div
        v-for="post in posts"
        :key="post.postId"
        class="feed-card"
        @click="$router.push(`/post/${post.postId}`)"
      >
        <el-image
          v-if="post.preview2dPath"
          :src="post.preview2dPath"
          fit="cover"
          class="card-cover"
        >
          <template #error>
            <div class="card-cover-placeholder">
              <el-icon size="48"><PictureFilled /></el-icon>
              <span>古建掠影</span>
            </div>
          </template>
        </el-image>
        <div v-else class="card-cover-placeholder">
          <el-icon size="48"><PictureFilled /></el-icon>
          <span>古建掠影</span>
        </div>

        <div class="card-body">
          <div class="card-title">{{ post.title }}</div>
          <div class="card-meta">
            <span class="author">
              <el-avatar size="20" :icon="UserFilled" />
              {{ post.authorNickname }}
            </span>
            <span class="time">{{ post.createdAt }}</span>
          </div>
          <div class="card-actions">
            <button class="action-btn" :class="{ liked: false }" @click.stop>
              <el-icon><StarFilled /></el-icon> {{ post.likeCount }}
            </button>
            <button class="action-btn" @click.stop>
              <el-icon><ChatDotRound /></el-icon> {{ post.commentCount }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <el-empty v-if="!loading && posts.length === 0" description="社区暂无动态，快去创造第一个数字古建吧">
      <el-button type="primary" @click="$router.push('/huanzhu')">一键幻筑</el-button>
    </el-empty>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchPosts"
        background
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { PictureFilled, StarFilled, ChatDotRound, MagicStick, UserFilled } from '@element-plus/icons-vue'
import { communityApi } from '@/api/community'

const posts = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)

onMounted(() => fetchPosts())

async function fetchPosts() {
  loading.value = true
  try {
    const res = await communityApi.getPosts(currentPage.value, pageSize.value)
    posts.value = res.data.records || []
    total.value = res.data.total || 0
  } catch { /* 拦截器已处理 */ }
  finally { loading.value = false }
}
</script>

<style scoped>
.culture-banner {
  text-align: center;
  padding: 48px 24px 32px;
  background: linear-gradient(180deg, rgba(44,24,16,0.03) 0%, transparent 100%);
}

.banner-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text-primary);
  letter-spacing: 4px;
  margin-bottom: 8px;
}

.banner-sub {
  font-size: 15px;
  color: var(--color-text-secondary);
  margin-bottom: 24px;
}

.card-cover {
  width: 100%;
  min-height: 180px;
  background: var(--color-bg-subtle);
}

.card-cover-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 180px;
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
  gap: 8px;
  font-size: 14px;
}

.author {
  display: flex;
  align-items: center;
  gap: 6px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 24px;
}
</style>
