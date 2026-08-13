<template>
  <div class="community-page animate-fade-in">
    <!-- 页面头部 -->
    <header class="page-header">
      <div class="header-text">
        <h2 class="header-title">文化游廊</h2>
        <p class="header-sub">穿梭千年的文明记忆，共赏木石之魂。</p>
      </div>
      <div class="header-search">
        <el-input
          v-model="searchText"
          placeholder="搜索朝代、形制、名胜..."
          :prefix-icon="Search"
          size="large"
          class="search-input"
          clearable
        />
      </div>
    </header>

    <!-- 操作行 -->
    <div class="action-bar">
      <el-button type="primary" size="large" @click="$router.push('/huanzhu')" class="btn-gradient">
        <el-icon><MagicStick /></el-icon>
        一键幻筑，创造你的古建
      </el-button>
      <el-button v-if="userStore.isLoggedIn" size="large" class="btn-publish" @click="showPublishDialog = true">
        <el-icon><Edit /></el-icon>
        发布古建动态
      </el-button>
    </div>

    <!-- 瀑布流 -->
    <div class="architecture-feed-container" v-loading="loading">
      <div
        v-for="post in posts"
        :key="post.postId"
        class="feed-card"
        @click="$router.push(`/post/${post.postId}`)"
      >
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
              <button class="action-btn" :class="{ liked: post.likedByMe }" @click.stop="handleLike(post)">
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
    </div>

    <!-- 分页 -->
    <div v-if="total > pageSize" class="pagination-wrap">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        v-model:current-page="currentPage"
        @current-change="fetchPosts"
      />
    </div>

    <!-- 发帖 Dialog -->
    <el-dialog v-model="showPublishDialog" title="发布古建动态" width="680px" :close-on-click-modal="false" @closed="resetPublishForm" class="publish-dialog">
      <el-form :model="publishForm" label-position="top" class="publish-form">
        <!-- 标题 -->
        <el-form-item label="标题">
          <el-input
            v-model="publishForm.title"
            placeholder="给你的古建记忆起个名字"
            maxlength="128"
            show-word-limit
            size="large"
          />
        </el-form-item>

        <!-- 封面图上传 -->
        <el-form-item label="封面图">
          <ImageUploader
            v-model="publishForm.preview2dPath"
            placeholder="上传封面图（可选）"
            :show-url-input="true"
            sub-dir="covers"
          />
        </el-form-item>

        <!-- 富文本编辑器 -->
        <el-form-item label="正文">
          <div class="editor-wrapper" v-if="showPublishDialog">
            <QuillEditor
              v-model:content="publishForm.content"
              content-type="html"
              :options="editorOptions"
              style="min-height: 200px;"
            />
          </div>
          <div class="editor-footer">
            <span class="char-count">已输入 {{ contentLength }} / 5000 字</span>
          </div>
        </el-form-item>

        <!-- 标签输入 -->
        <el-form-item label="标签">
          <div class="tags-input-wrapper">
            <el-tag
              v-for="tag in publishForm.tags"
              :key="tag"
              closable
              type="primary"
              @close="removeTag(tag)"
              class="tag-item"
            >
              {{ tag }}
            </el-tag>
            <el-input
              v-if="showTagInput"
              ref="tagInputRef"
              v-model="newTag"
              size="small"
              style="width: 120px;"
              @keyup.enter="addTag"
              @blur="addTag"
              placeholder="输入标签"
            />
            <el-button v-else size="small" @click="showTagInput = true" :icon="Plus">
              添加标签
            </el-button>
          </div>
          <div class="suggested-tags">
            <span class="label">推荐：</span>
            <el-tag
              v-for="tag in suggestedTags"
              :key="tag"
              size="small"
              type="info"
              effect="plain"
              @click="addSuggestedTag(tag)"
              class="suggest-tag"
            >
              {{ tag }}
            </el-tag>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPublishDialog = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="publishPost" class="btn-publish-submit">
          发布动态
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, defineAsyncComponent } from 'vue'
import { useRouter } from 'vue-router'
import { MagicStick, Edit, Search, PictureFilled, UserFilled, Star, StarFilled, ChatLineSquare, Plus } from '@element-plus/icons-vue'
import { communityApi } from '@/api/community'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import ImageUploader from '@/components/ImageUploader.vue'

const router = useRouter()
const userStore = useUserStore()

// 懒加载富文本编辑器，仅在打开发布弹窗时下载
const QuillEditor = defineAsyncComponent(async () => {
  await import('@vueup/vue-quill/dist/vue-quill.snow.css')
  return import('@vueup/vue-quill')
})

const posts = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = 12
const searchText = ref('')

const showPublishDialog = ref(false)
const publishing = ref(false)
const publishForm = ref({
  title: '',
  preview2dPath: '',
  content: '',
  tags: []
})

// 标签相关
const showTagInput = ref(false)
const newTag = ref('')
const tagInputRef = ref(null)
const suggestedTags = ['唐代', '宋代', '明代', '清代', '斗栱', '歇山顶', '庑殿顶', '悬山顶', '佛光寺', '故宫', '园林', '民居']

// 富文本编辑器配置
const editorOptions = {
  placeholder: '讲述你与这座建筑的相遇...',
  modules: {
    toolbar: [
      ['bold', 'italic', 'underline', 'strike'],
      ['blockquote', 'code-block'],
      [{ 'header': 1 }, { 'header': 2 }],
      [{ 'list': 'ordered' }, { 'list': 'bullet' }],
      [{ 'indent': '-1' }, { 'indent': '+1' }],
      ['link', 'image'],
      ['clean']
    ]
  },
  theme: 'snow'
}

// 计算正文长度
const contentLength = computed(() => {
  // 去除 HTML 标签后计算长度
  const text = publishForm.value.content.replace(/<[^>]*>/g, '')
  return text.length
})

/* ═══ 数据 ═══ */
function parseTags(tags) {
  if (!tags) return []
  if (Array.isArray(tags)) return tags
  return tags.split(',').map(t => t.trim()).filter(Boolean)
}

async function fetchPosts() {
  loading.value = true
  try {
    const res = await communityApi.getPosts({
      page: currentPage.value,
      size: pageSize,
      keyword: searchText.value || undefined
    })
    posts.value = res.data.records || res.data || []
    total.value = res.data.total || 0
  } catch { /* ignore */ }
  loading.value = false
}

async function handleLike(post) {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  const prevLiked = post.likedByMe
  const prevCount = post.likeCount || 0
  post.likedByMe = !post.likedByMe
  post.likeCount = prevCount + (post.likedByMe ? 1 : -1)
  try {
    await communityApi.toggleLike({ targetId: post.postId, targetType: 'POST' })
  } catch {
    post.likedByMe = prevLiked
    post.likeCount = prevCount
  }
}

/* ═══ 标签操作 ═══ */
function addTag() {
  const tag = newTag.value.trim()
  if (tag && !publishForm.value.tags.includes(tag)) {
    if (publishForm.value.tags.length >= 5) {
      ElMessage.warning('最多添加 5 个标签')
      return
    }
    publishForm.value.tags.push(tag)
  }
  newTag.value = ''
  showTagInput.value = false
}

function removeTag(tag) {
  publishForm.value.tags = publishForm.value.tags.filter(t => t !== tag)
}

function addSuggestedTag(tag) {
  if (!publishForm.value.tags.includes(tag)) {
    if (publishForm.value.tags.length >= 5) {
      ElMessage.warning('最多添加 5 个标签')
      return
    }
    publishForm.value.tags.push(tag)
  }
}

/* ═══ 发帖 ═══ */
async function publishPost() {
  const { title, content, preview2dPath, tags } = publishForm.value
  if (!title.trim()) return ElMessage.warning('请输入标题')
  if (!content || content === '<p><br></p>') return ElMessage.warning('请输入正文内容')
  if (contentLength.value > 5000) return ElMessage.warning('正文内容不能超过 5000 字')

  publishing.value = true
  try {
    await communityApi.createPost({
      title,
      content,
      preview2dPath: preview2dPath || null,
      tags: tags.length > 0 ? tags.join(',') : null
    })
    ElMessage.success('发布成功，等待审核')
    showPublishDialog.value = false
    resetPublishForm()
    fetchPosts()
  } catch { /* ignore */ }
  finally {
    publishing.value = false
  }
}

function resetPublishForm() {
  publishForm.value = { title: '', preview2dPath: '', content: '', tags: [] }
  showTagInput.value = false
  newTag.value = ''
}

onMounted(fetchPosts)
</script>

<style scoped>
.community-page {
  height: 100%;
  overflow-y: auto;
  padding: var(--spacing-xl);
}

.community-page::-webkit-scrollbar { width: 6px; }
.community-page::-webkit-scrollbar-track { background: transparent; }
.community-page::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.1); border-radius: 3px; }
.community-page::-webkit-scrollbar-thumb:hover { background: rgba(0,0,0,0.18); }

[data-theme="dark"] .community-page::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.08); }
[data-theme="dark"] .community-page::-webkit-scrollbar-thumb:hover { background: rgba(255,255,255,0.14); }

/* ═══ 头部 ═══ */
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: var(--spacing-lg);
  gap: var(--spacing-lg);
}

.header-title {
  font-family: var(--font-family-serif);
  font-size: var(--font-size-title);
  color: var(--color-text-main);
  margin-bottom: 4px;
}

.header-sub {
  font-size: 14px;
  color: var(--color-text-sub);
}

.header-search {
  width: 300px;
  flex-shrink: 0;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: var(--radius-full);
  background: var(--color-surface);
  backdrop-filter: blur(8px);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-card);
}

/* ═══ 操作行 ═══ */
.action-bar {
  display: flex;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-xl);
}

.btn-publish {
  border-color: var(--color-accent);
  color: var(--color-accent);
}

.btn-publish:hover {
  background: var(--color-accent-soft);
  border-color: var(--color-accent);
}

/* ═══ 卡片封面 ═══ */
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

/* ═══ 分页 ═══ */
.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: var(--spacing-xl);
}

/* ═══ 发帖对话框 ═══ */
.publish-dialog :deep(.el-dialog) {
  border-radius: var(--radius-xl);
}

.publish-dialog :deep(.el-dialog__header) {
  padding: var(--spacing-lg) var(--spacing-xl);
  border-bottom: 1px solid var(--color-border);
  margin: 0;
}

.publish-dialog :deep(.el-dialog__body) {
  padding: var(--spacing-xl);
}

.publish-dialog :deep(.el-dialog__footer) {
  padding: var(--spacing-md) var(--spacing-xl) var(--spacing-lg);
  border-top: 1px solid var(--color-border);
}

.publish-form {
  max-height: 60vh;
  overflow-y: auto;
  padding-right: var(--spacing-sm);
}

.publish-form::-webkit-scrollbar { width: 4px; }
.publish-form::-webkit-scrollbar-track { background: transparent; }
.publish-form::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.1); border-radius: 2px; }

/* ═══ 富文本编辑器 ═══ */
.editor-wrapper {
  width: 100%;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--color-border);
}

.editor-wrapper :deep(.ql-toolbar) {
  border: none;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-subtle);
}

.editor-wrapper :deep(.ql-container) {
  border: none;
  font-family: var(--font-family-base);
  font-size: 14px;
}

.editor-wrapper :deep(.ql-editor) {
  min-height: 200px;
  padding: var(--spacing-md);
}

.editor-wrapper :deep(.ql-editor.ql-blank::before) {
  color: var(--color-text-muted);
  font-style: normal;
}

.editor-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--spacing-xs);
}

.char-count {
  font-size: 12px;
  color: var(--color-text-muted);
}

/* ═══ 标签输入 ═══ */
.tags-input-wrapper {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
  align-items: center;
  padding: var(--spacing-sm);
  background: var(--color-bg-subtle);
  border-radius: var(--radius-md);
  min-height: 48px;
}

.tag-item {
  margin: 0;
}

.suggested-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-xs);
  align-items: center;
  margin-top: var(--spacing-sm);
}

.suggested-tags .label {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-right: var(--spacing-xs);
}

.suggest-tag {
  cursor: pointer;
  transition: all var(--transition-fast);
}

.suggest-tag:hover {
  color: var(--color-accent);
  border-color: var(--color-accent);
  background: var(--color-accent-soft);
}

.btn-publish-submit {
  min-width: 100px;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
  }
  .header-search {
    width: 100%;
  }
  .action-bar {
    flex-wrap: wrap;
  }
  .publish-dialog :deep(.el-dialog) {
    width: 95% !important;
    margin: 0 auto;
  }
}
</style>
