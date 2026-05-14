<template>
  <div class="community-feed">
    <!-- 文化标语横幅 -->
    <div class="culture-banner">
      <h2 class="banner-title">以技术为舟，载文化远航</h2>
      <p class="banner-sub">每一块瓦当、每一组斗栱，在数字世界里重获新生</p>
      <div class="banner-actions">
        <el-button type="primary" size="large" @click="$router.push('/huanzhu')">
          <el-icon><MagicStick /></el-icon>
          一键幻筑，创造你的古建
        </el-button>
        <el-button v-if="userStore.isLoggedIn" size="large" class="btn-publish" @click="showPublishDialog = true">
          <el-icon><Edit /></el-icon>
          发布古建动态
        </el-button>
      </div>
    </div>

    <!-- 帖子瀑布流 — CSS Grid 动态高度 -->
    <div
      ref="feedContainerRef"
      class="architecture-feed-container"
      v-loading="loading"
    >
      <div
        v-for="post in posts"
        :key="post.postId"
        class="feed-card"
        :ref="el => setCardRef(post.postId, el)"
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
            <button class="action-btn" :class="{ liked: post.likedByMe }" @click.stop="handleLike(post)">
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

    <!-- 发帖弹窗 -->
    <el-dialog
      v-model="showPublishDialog"
      title="发布古建动态"
      width="600px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form :model="postForm" :rules="postRules" ref="postFormRef" label-position="top">
        <el-form-item label="标题" prop="title">
          <el-input v-model="postForm.title" placeholder="给你的古建作品起个名字..." maxlength="128" show-word-limit />
        </el-form-item>

        <el-form-item label="文化描述" prop="content">
          <el-input
            v-model="postForm.content"
            type="textarea"
            :rows="5"
            placeholder="分享你的古建知识、创作心得或文化见解..."
            maxlength="2048"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="关联3D模型 (可选)">
          <el-input v-model="postForm.modelAssetId" placeholder="填入幻筑生成的模型资产ID" />
          <div class="form-tip">一键幻筑成功后可在「数字锦盒」中查看资产ID</div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showPublishDialog = false">取 消</el-button>
        <el-button type="primary" :loading="publishing" @click="handlePublish">
          <el-icon><Promotion /></el-icon>
          发布动态
        </el-button>
      </template>
    </el-dialog>

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
import { ref, reactive, onMounted, nextTick, watch } from 'vue'
import { PictureFilled, StarFilled, ChatDotRound, MagicStick, UserFilled, Edit, Promotion } from '@element-plus/icons-vue'
import { communityApi } from '@/api/community'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()

const posts = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)
const feedContainerRef = ref(null)
const cardRefs = {}

function setCardRef(id, el) {
  if (el) cardRefs[id] = el
}

/** 根据卡片实际高度动态计算 grid-row-end */
function calcCardSpans() {
  nextTick(() => {
    posts.value.forEach(post => {
      const el = cardRefs[post.postId]
      if (!el) return
      const height = el.offsetHeight
      const rows = Math.ceil(height / 10)
      el.style.gridRowEnd = `span ${rows}`
    })
  })
}

watch(posts, () => calcCardSpans())

// ===== 发帖 =====
const showPublishDialog = ref(false)
const publishing = ref(false)
const postFormRef = ref(null)

const postForm = reactive({
  title: '',
  content: '',
  modelAssetId: ''
})

const postRules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 2, max: 128, message: '标题长度 2-128 字符', trigger: 'blur' }
  ]
}

async function handlePublish() {
  const valid = await postFormRef.value.validate().catch(() => false)
  if (!valid) return

  publishing.value = true
  try {
    const payload = {
      title: postForm.title.trim(),
      content: postForm.content.trim(),
      modelAssetId: postForm.modelAssetId ? Number(postForm.modelAssetId) : null
    }
    await communityApi.createPost(payload)
    ElMessage.success('动态已提交，待管理员审核通过后公开展示')
    showPublishDialog.value = false
    postForm.title = ''
    postForm.content = ''
    postForm.modelAssetId = ''
    await fetchPosts()
  } finally {
    publishing.value = false
  }
}

/** 在瀑布流中直接点赞 */
async function handleLike(post) {
  try {
    await communityApi.toggleLike({ targetId: post.postId, targetType: 'POST' })
    post.likedByMe = !post.likedByMe
    post.likeCount += post.likedByMe ? 1 : -1
  } catch { /* ignore */ }
}

onMounted(() => fetchPosts())

async function fetchPosts() {
  loading.value = true
  try {
    const res = await communityApi.getPosts(currentPage.value, pageSize.value)
    posts.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* 文化横幅 — 大面积留白，禅意克制 */
.culture-banner {
  text-align: center;
  padding: 48px var(--spacing-lg) var(--spacing-xl);
  background: linear-gradient(180deg, rgba(0, 21, 41, 0.03) 0%, transparent 100%);
}

.banner-title {
  font-size: var(--font-size-title);
  font-weight: 700;
  color: var(--color-text-main);
  letter-spacing: 4px;
  margin-bottom: var(--spacing-sm);
}

.banner-sub {
  font-size: 15px;
  color: var(--color-text-sub);
  margin-bottom: var(--spacing-lg);
  line-height: var(--line-height-body);
}

.banner-actions {
  display: flex;
  justify-content: center;
  gap: var(--spacing-md);
  flex-wrap: wrap;
}

.btn-publish {
  border-color: var(--color-secondary);
  color: var(--color-secondary);
}
.btn-publish:hover {
  background: rgba(181, 142, 54, 0.1);
  border-color: var(--color-secondary);
}

.form-tip {
  font-size: var(--font-size-caption);
  color: var(--color-text-muted);
  margin-top: var(--spacing-xs);
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
  gap: var(--spacing-sm);
  font-size: 14px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: var(--spacing-lg) 0;
}
</style>
