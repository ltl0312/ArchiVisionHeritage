<template>
  <div class="profile-page">
    <!-- 用户信息卡片 -->
    <div class="profile-header">
      <div class="profile-cover"></div>
      <div class="profile-info">
        <el-avatar :size="80" :icon="UserFilled" :src="user.avatarUrl" class="profile-avatar" />
        <div class="profile-text">
          <h2 class="nickname">{{ user.nickname || '古建爱好者' }}</h2>
          <p class="username">@{{ user.username }}</p>
          <p class="bio">{{ user.bio || '这个人很懒，还没有留下文化签名...' }}</p>
          <el-tag v-if="user.role === 'ADMIN'" type="danger" size="small" effect="dark">
            管理员
          </el-tag>
        </div>
        <el-button class="edit-btn" @click="$router.push('/settings')">
          <el-icon><Edit /></el-icon>
          编辑资料
        </el-button>
      </div>
    </div>

    <!-- Tab 切换区域 -->
    <div class="profile-tabs">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <!-- 我发布的古建帖子 -->
        <el-tab-pane label="我的帖子" name="posts">
          <div class="tab-grid" v-loading="loading">
            <div
              v-for="post in posts"
              :key="post.postId"
              class="feed-card"
              @click="$router.push(`/post/${post.postId}`)"
            >
              <el-image v-if="post.preview2dPath" :src="post.preview2dPath" fit="cover" class="card-cover">
                <template #error>
                  <div class="card-cover-placeholder">
                    <el-icon size="36"><PictureFilled /></el-icon>
                  </div>
                </template>
              </el-image>
              <div v-else class="card-cover-placeholder">
                <el-icon size="36"><PictureFilled /></el-icon>
              </div>
              <div class="card-body">
                <div class="card-title">{{ post.title }}</div>
                <div class="card-meta">
                  <span>{{ post.createdAt }}</span>
                  <span class="card-stats">
                    <el-icon><StarFilled /></el-icon> {{ post.likeCount }}
                    <el-icon><ChatDotRound /></el-icon> {{ post.commentCount }}
                  </span>
                </div>
              </div>
            </div>
            <el-empty v-if="!loading && posts.length === 0" description="还没有发布过帖子" />
          </div>
        </el-tab-pane>

        <!-- 我点赞的记录 -->
        <el-tab-pane label="我的点赞" name="likes">
          <div class="tab-grid" v-loading="loading">
            <div
              v-for="post in posts"
              :key="post.postId"
              class="feed-card"
              @click="$router.push(`/post/${post.postId}`)"
            >
              <el-image v-if="post.preview2dPath" :src="post.preview2dPath" fit="cover" class="card-cover">
                <template #error>
                  <div class="card-cover-placeholder">
                    <el-icon size="36"><PictureFilled /></el-icon>
                  </div>
                </template>
              </el-image>
              <div v-else class="card-cover-placeholder">
                <el-icon size="36"><PictureFilled /></el-icon>
              </div>
              <div class="card-body">
                <div class="card-title">{{ post.title }}</div>
                <div class="card-meta">
                  <span>{{ post.authorNickname }}</span>
                  <span>{{ post.createdAt }}</span>
                </div>
              </div>
            </div>
            <el-empty v-if="!loading && posts.length === 0" description="还没有点赞过帖子" />
          </div>
        </el-tab-pane>

        <!-- 我的3D数字锦盒 -->
        <el-tab-pane label="数字锦盒" name="boxes">
          <div class="tab-grid" v-loading="loading">
            <div
              v-for="post in posts"
              :key="post.postId"
              class="feed-card"
              @click="$router.push(`/post/${post.postId}`)"
            >
              <el-image v-if="post.preview2dPath" :src="post.preview2dPath" fit="cover" class="card-cover">
                <template #error>
                  <div class="card-cover-placeholder">
                    <el-icon size="36"><PictureFilled /></el-icon>
                  </div>
                </template>
              </el-image>
              <div v-else class="card-cover-placeholder">
                <el-icon size="36"><PictureFilled /></el-icon>
              </div>
              <div class="card-body">
                <div class="card-title">{{ post.title }}</div>
                <div class="card-meta">
                  <span>{{ post.createdAt }}</span>
                </div>
              </div>
            </div>
            <el-empty v-if="!loading && posts.length === 0" description="还没有生成数字古建">
              <el-button type="primary" @click="$router.push('/huanzhu')">一键幻筑</el-button>
            </el-empty>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { UserFilled, Edit, PictureFilled, StarFilled, ChatDotRound } from '@element-plus/icons-vue'
import { authApi } from '@/api/auth'
import { ElMessage } from 'element-plus'

const activeTab = ref('posts')
const loading = ref(false)
const posts = ref([])
const user = ref({
  nickname: '',
  username: '',
  bio: '',
  avatarUrl: '',
  role: 'USER'
})

onMounted(async () => {
  await fetchUserInfo()
  fetchTabData()
})

async function fetchUserInfo() {
  try {
    const res = await authApi.getCurrentUser()
    user.value = res.data
  } catch { /* ignore */ }
}

async function fetchTabData() {
  loading.value = true
  try {
    if (activeTab.value === 'posts') {
      const res = await authApi.getMyPosts()
      posts.value = res.data?.records || []
    } else if (activeTab.value === 'likes') {
      const res = await authApi.getMyLikes()
      posts.value = res.data?.records || []
    } else if (activeTab.value === 'boxes') {
      // 数字锦盒 = 自己发布的且有 model_asset_id 的帖子
      const res = await authApi.getMyPosts()
      posts.value = (res.data?.records || []).filter(p => p.preview2dPath)
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function onTabChange(tab) {
  activeTab.value = tab
  posts.value = []
  fetchTabData()
}
</script>

<style scoped>
.profile-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px;
}

.profile-header {
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  margin-bottom: 24px;
}

.profile-cover {
  height: 120px;
  background: linear-gradient(135deg, #2c1810 0%, #4a2c17 50%, #6b3a2a 100%);
}

.profile-info {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  padding: 0 24px 24px;
  margin-top: -40px;
  position: relative;
}

.profile-avatar {
  border: 4px solid white;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.profile-text {
  flex: 1;
  padding-top: 44px;
}

.nickname {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: 4px;
}

.username {
  font-size: 14px;
  color: var(--color-text-muted);
  margin-bottom: 8px;
}

.bio {
  font-size: 14px;
  color: var(--color-text-secondary);
  font-style: italic;
  line-height: 1.6;
}

.edit-btn {
  margin-top: 44px;
}

.profile-tabs {
  background: var(--color-bg-card);
  border-radius: var(--radius-lg);
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  padding: 8px 24px 24px;
  min-height: 400px;
}

.tab-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
@media (max-width: 768px) { .tab-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 480px) { .tab-grid { grid-template-columns: 1fr; } }

.card-cover { width: 100%; min-height: 140px; background: var(--color-bg-subtle); }

.card-cover-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 140px;
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
}

.card-stats {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
}
.card-stats .el-icon { font-size: 13px; }
</style>
