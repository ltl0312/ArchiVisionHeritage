<template>
  <div class="admin-page animate-fade-in">
    <div class="admin-header">
      <h2 class="page-title">
        <el-icon><Monitor /></el-icon>
        中枢控制台
      </h2>
      <p class="page-sub">仅掌印官（管理员）可见的数据总览与权限管理中心。</p>
    </div>

    <!-- 审核队列 -->
    <div class="glass-card" v-loading="loading">
      <h3 class="section-title">待审核帖子</h3>

      <div v-if="pendingPosts.length" class="audit-list">
        <div v-for="post in pendingPosts" :key="post.postId" class="audit-item">
          <div class="audit-info">
            <h4>{{ post.title }}</h4>
            <div class="audit-meta">
              <span>作者：{{ post.authorNickname || '匿名' }}</span>
              <span>{{ post.createdAt }}</span>
            </div>
            <p class="audit-content">{{ (post.content || '').substring(0, 120) }}{{ (post.content || '').length > 120 ? '...' : '' }}</p>
          </div>
          <div class="audit-actions">
            <el-button type="success" :loading="auditingId === post.postId" @click="auditPost(post.postId, true)">
              通过
            </el-button>
            <el-button type="danger" :loading="auditingId === post.postId" @click="auditPost(post.postId, false)">
              驳回
            </el-button>
          </div>
        </div>
      </div>

      <el-empty v-else description="暂无待审核帖子" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Monitor } from '@element-plus/icons-vue'
import { adminApi } from '@/api/admin'
import { ElMessage } from 'element-plus'

const loading = ref(true)
const pendingPosts = ref([])
const auditingId = ref(null)

async function fetchPending() {
  loading.value = true
  try {
    const res = await adminApi.getPendingPosts()
    pendingPosts.value = res.data?.records || res.data || []
  } catch { /* ignore */ }
  loading.value = false
}

async function auditPost(postId, approved) {
  auditingId.value = postId
  try {
    await adminApi.auditPost(postId, approved ? 'APPROVED' : 'REJECTED')
    ElMessage.success(approved ? '已通过' : '已驳回')
    fetchPending()
  } catch { /* ignore */ }
  auditingId.value = null
}

onMounted(fetchPending)
</script>

<style scoped>
.admin-page {
  height: 100%;
  overflow-y: auto;
  padding: var(--spacing-xl);
}

.admin-page::-webkit-scrollbar { width: 6px; }
.admin-page::-webkit-scrollbar-track { background: transparent; }
.admin-page::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.1); border-radius: 3px; }

.admin-header {
  margin-bottom: var(--spacing-xl);
}

.page-title {
  font-family: var(--font-family-serif);
  font-size: var(--font-size-title);
  color: var(--color-text-main);
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: 4px;
}

.page-sub {
  font-size: 14px;
  color: var(--color-text-sub);
}

.glass-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-2xl);
  padding: var(--spacing-xl);
}

.section-title {
  font-family: var(--font-family-serif);
  font-size: 18px;
  color: var(--color-text-main);
  margin-bottom: var(--spacing-lg);
}

.audit-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--spacing-lg);
  padding: var(--spacing-md) 0;
  border-bottom: 1px solid var(--color-border);
}

.audit-info { flex: 1; }
.audit-info h4 {
  font-size: 15px;
  color: var(--color-text-main);
  margin-bottom: 4px;
}

.audit-meta {
  font-size: 13px;
  color: var(--color-text-muted);
  display: flex;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-sm);
}

.audit-content {
  font-size: 13px;
  color: var(--color-text-sub);
  line-height: 1.6;
}

.audit-actions {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
  flex-shrink: 0;
}
</style>
