<template>
  <div class="admin-page">
    <div class="admin-header">
      <h2 class="page-title">
        <el-icon><Monitor /></el-icon>
        审核工作台
      </h2>
      <el-tag type="info" size="large">待审核 {{ total }} 篇</el-tag>
    </div>

    <!-- 审核列表 -->
    <div class="audit-list" v-loading="loading">
      <div v-for="post in posts" :key="post.postId" class="audit-card">
        <div class="audit-card-cover">
          <el-image v-if="post.preview2dPath" :src="post.preview2dPath" fit="cover" class="cover-img">
            <template #error>
              <div class="cover-placeholder">
                <el-icon size="40"><PictureFilled /></el-icon>
              </div>
            </template>
          </el-image>
          <div v-else class="cover-placeholder">
            <el-icon size="40"><PictureFilled /></el-icon>
          </div>
        </div>

        <div class="audit-card-body">
          <h4 class="post-title">{{ post.title }}</h4>
          <div class="post-meta">
            <el-avatar :size="20" :icon="UserFilled" />
            <span>{{ post.authorNickname }}</span>
            <el-divider direction="vertical" />
            <span class="post-time">{{ post.createdAt }}</span>
          </div>
          <div class="post-stats">
            <span><el-icon><StarFilled /></el-icon> {{ post.likeCount }}</span>
            <span><el-icon><ChatDotRound /></el-icon> {{ post.commentCount }}</span>
          </div>

          <!-- 驳回原因输入（仅在选择驳回时展示） -->
          <div v-if="rejectingId === post.postId" class="reject-area">
            <el-input
              v-model="rejectReason"
              placeholder="请填写驳回原因..."
              maxlength="255"
              class="reject-input"
            />
            <div class="reject-actions">
              <el-button size="small" @click="rejectingId = null">取消</el-button>
              <el-button size="small" type="danger" @click="confirmReject(post.postId)">
                确认驳回
              </el-button>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="audit-actions" v-if="rejectingId !== post.postId">
            <el-button type="success" :loading="auditingId === post.postId" @click="approve(post.postId)">
              <el-icon><Select /></el-icon>
              审核通过
            </el-button>
            <el-button type="danger" plain :loading="auditingId === post.postId" @click="startReject(post.postId)">
              <el-icon><CloseBold /></el-icon>
              驳回
            </el-button>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <el-empty v-if="!loading && posts.length === 0" description="暂无待审核内容">
        <template #image>
          <el-icon size="64" color="#c0c4cc"><CircleCheckFilled /></el-icon>
        </template>
      </el-empty>
    </div>

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
import { Monitor, PictureFilled, StarFilled, ChatDotRound, Select, CloseBold, CircleCheckFilled, UserFilled } from '@element-plus/icons-vue'
import { adminApi } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

const posts = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const auditingId = ref(null)
const rejectingId = ref(null)
const rejectReason = ref('')

onMounted(() => fetchPosts())

async function fetchPosts() {
  loading.value = true
  try {
    const res = await adminApi.getPendingPosts(currentPage.value, pageSize.value)
    posts.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function approve(postId) {
  auditingId.value = postId
  try {
    await adminApi.auditPost(postId, 'APPROVED', '')
    ElMessage.success('已审核通过，帖子已发布至社区信息流')
    posts.value = posts.value.filter(p => p.postId !== postId)
    total.value--
  } finally {
    auditingId.value = null
  }
}

function startReject(postId) {
  rejectingId.value = postId
  rejectReason.value = ''
}

async function confirmReject(postId) {
  if (!rejectReason.value.trim()) {
    return ElMessage.warning('请填写驳回原因')
  }
  auditingId.value = postId
  try {
    await adminApi.auditPost(postId, 'REJECTED', rejectReason.value)
    ElMessage.success('已驳回该帖子')
    posts.value = posts.value.filter(p => p.postId !== postId)
    total.value--
    rejectingId.value = null
  } finally {
    auditingId.value = null
  }
}
</script>

<style scoped>
.admin-page {
  max-width: 960px;
  margin: 0 auto;
  padding: var(--spacing-lg);
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-lg);
}

.page-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: var(--font-size-title);
  font-weight: 700;
  color: var(--color-text-main);
  letter-spacing: 2px;
}

.audit-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.audit-card {
  display: flex;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-card);
  transition: box-shadow var(--transition-normal);
}
.audit-card:hover {
  box-shadow: var(--shadow-card-hover);
}

.audit-card-cover {
  width: 200px;
  min-height: 160px;
  flex-shrink: 0;
}

.cover-img {
  width: 100%;
  height: 100%;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
}

.audit-card-body {
  flex: 1;
  padding: var(--spacing-md) var(--spacing-lg);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.post-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--color-text-main);
  margin-bottom: var(--spacing-sm);
}

.post-meta {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: 13px;
  color: var(--color-text-sub);
  margin-bottom: var(--spacing-xs);
}

.post-time { color: var(--color-text-muted); }

.post-stats {
  display: flex;
  gap: var(--spacing-md);
  font-size: 13px;
  color: var(--color-text-sub);
}

.audit-actions {
  display: flex;
  gap: var(--spacing-sm);
  margin-top: var(--spacing-sm);
}

.reject-area {
  margin-top: var(--spacing-sm);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.reject-input { max-width: 360px; }

.reject-actions {
  display: flex;
  gap: var(--spacing-sm);
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: var(--spacing-lg) 0;
}

@media (max-width: 640px) {
  .audit-card { flex-direction: column; }
  .audit-card-cover { width: 100%; height: 180px; }
}
</style>
