<template>
  <div class="view-shell">
    <!-- ═══════════ 工具行：真实统计 + 当前页筛选 + 发帖入口 ═══════════ -->
    <div class="v4-toolrow">
      <div class="v4-headstats">
        <span v-if="error" class="v4-pill v4-pill--red">列表加载失败</span>
        <template v-else>
          <span class="v4-pill v4-pill--gold">公开档案 {{ total }} 件</span>
          <span v-if="posts.length" class="v4-pill v4-pill--mut v4-pill--bare">
            本页 {{ posts.length }} 件
          </span>
          <span v-if="loading && posts.length" class="v4-pill v4-pill--jade">
            正在加载第 {{ currentPage }} 页
          </span>
        </template>
      </div>

      <div class="cs-tools">
        <!-- 与顶栏检索框同源：都只写 route.query.q（后端不支持关键词参数） -->
        <form class="v4-search cs-search" role="search" @submit.prevent="submitFilter">
          <el-icon :size="16"><Search /></el-icon>
          <input
            v-model="keyword"
            type="search"
            class="cs-search-input"
            placeholder="在当前页档案中筛选"
            aria-label="在当前页已加载的档案中筛选"
          />
          <button
            v-if="keyword"
            type="button"
            class="cs-search-clear"
            aria-label="清空筛选词"
            @click="clearKeyword"
          >
            <el-icon :size="14"><Close /></el-icon>
          </button>
        </form>

        <button class="v4-btn v4-btn--ghost v4-btn--sm" type="button" @click="goHuanZhu">
          <el-icon :size="15"><MagicStick /></el-icon>
          去一键幻筑
        </button>
        <button
          v-if="userStore.isLoggedIn"
          class="v4-btn v4-btn--gold v4-btn--sm"
          type="button"
          @click="openPublish"
        >
          <el-icon :size="15"><Edit /></el-icon>
          发布动态
        </button>
      </div>
    </div>

    <!-- ═══════════ 筛选声明：如实说明「只在已加载的当前页筛选」 ═══════════ -->
    <div v-if="activeKeyword" class="cs-filterrow">
      <button class="v4-pill v4-pill--mut cs-filterpill" type="button" @click="clearKeyword">
        在当前已加载的 {{ posts.length }} 条中筛选「{{ activeKeyword }}」 · 清除
      </button>
      <span class="cs-filternote">
        后端档案流暂不支持关键词查询，检索只在当前页已加载的档案中生效，未覆盖全站。
      </span>
    </div>

    <!-- ═══════════ 发帖入口：克制的面板（登录后） ═══════════ -->
    <div v-if="userStore.isLoggedIn" class="v4-card cs-compose">
      <div class="v4-cardhd">
        <h3>发起分享</h3>
        <span class="k">发布后进入待审队列 · 审核通过才公开展示</span>
      </div>
      <div class="v4-textarea cs-entry">
        <button class="cs-entry-btn" type="button" @click="openPublish">
          讲述你与这座建筑的相遇…
        </button>
        <div class="foot">
          <span>富文本正文 · 封面图（上传前自动压缩）· 最多 5 个标签</span>
          <button class="v4-btn v4-btn--gold v4-btn--sm" type="button" @click="openPublish">
            开始撰写
          </button>
        </div>
      </div>
    </div>

    <!-- 未登录：可匿名浏览，给出登录入口 -->
    <div v-else class="v4-tip cs-login-tip">
      <b>匿名浏览中</b>
      登录后即可发布你的数字档案、参与探讨与赞赏。
      <button class="v4-btn v4-btn--outline v4-btn--sm" type="button" @click="goLogin">
        去登录
      </button>
    </div>

    <!-- ═══════════ 分区标题 ═══════════ -->
    <div class="v4-sechead">
      <h2>大家都在做什么</h2>
      <span class="v4-pill v4-pill--mut v4-pill--bare">
        {{
          activeKeyword
            ? `命中 ${visiblePosts.length} / ${posts.length}`
            : `第 ${currentPage} 页`
        }}
      </span>
    </div>

    <!-- ═══════════ ① 加载中（首屏骨架） ═══════════ -->
    <div v-if="loading && !posts.length" class="cs-feed" role="status" aria-live="polite">
      <el-skeleton v-for="i in 6" :key="i" animated class="cs-skel">
        <template #template>
          <el-skeleton-item variant="image" class="cs-skel-img" />
          <div class="cs-skel-bd">
            <el-skeleton-item variant="h3" class="cs-skel-t" />
            <el-skeleton-item variant="text" class="cs-skel-m" />
          </div>
        </template>
      </el-skeleton>
      <p class="cs-skel-tip">
        <el-icon class="loading-spin" :size="14"><Loading /></el-icon>
        正在调取公开档案…
      </p>
    </div>

    <!-- ═══════════ ② 错误 ═══════════ -->
    <div v-else-if="error" class="v4-empty">
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M12 3.6 21 19.4H3z" />
        <path d="M12 10v4.2" />
        <path d="M12 17.1h.01" />
      </svg>
      <b class="cs-empty-t">{{ error }}</b>
      <p class="cs-empty-d">{{ errorDetail }}</p>
      <div class="cs-empty-acts">
        <button class="v4-btn v4-btn--gold" type="button" @click="load">重新加载</button>
      </div>
    </div>

    <!-- ═══════════ ③ 空（社区还没有内容） ═══════════ -->
    <div v-else-if="!posts.length" class="v4-empty">
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M3 8.6 12 3l9 5.6" />
        <path d="M5.4 9.4v9.2h13.2V9.4" />
        <path d="M9.6 18.6v-4.8h4.8v4.8" />
      </svg>
      <b class="cs-empty-t">社区还没有公开档案 · 成为第一个分享的人</b>
      <p class="cs-empty-d">发布你的古建数字档案，让每一块瓦当、每一组斗栱在社区里被看见。</p>
      <div class="cs-empty-acts">
        <button
          v-if="userStore.isLoggedIn"
          class="v4-btn v4-btn--gold"
          type="button"
          @click="openPublish"
        >
          发布第一条动态
        </button>
        <button v-else class="v4-btn v4-btn--gold" type="button" @click="goLogin">
          登录后发布
        </button>
        <button class="v4-btn v4-btn--ghost" type="button" @click="goHuanZhu">
          先去一键幻筑
        </button>
      </div>
    </div>

    <!-- ═══════════ ④ 筛选无结果（区别于空态） ═══════════ -->
    <div v-else-if="!visiblePosts.length" class="v4-empty">
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <circle cx="10.6" cy="10.6" r="6.4" />
        <path d="M15.4 15.4 20.4 20.4" />
      </svg>
      <b class="cs-empty-t">当前页没有匹配「{{ activeKeyword }}」的档案</b>
      <p class="cs-empty-d">
        本页已加载 {{ posts.length }} 条，均未命中该关键词。可清除筛选，或翻页后再试。
      </p>
      <div class="cs-empty-acts">
        <button class="v4-btn v4-btn--gold" type="button" @click="clearKeyword">清除筛选</button>
      </div>
    </div>

    <!-- ═══════════ 成功：信息流（保留信息流形态，不做档案卡网格） ═══════════ -->
    <div v-else class="cs-feed">
      <PostCard v-for="post in visiblePosts" :key="post.postId" :post="post" />
    </div>

    <!-- ═══════════ 分页 ═══════════ -->
    <div v-if="!error && total > pageSize" class="cs-pagination">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        @current-change="onPageChange"
      />
    </div>

    <!-- ═══════════ 发帖 Dialog（Quill 富文本 + ImageUploader） ═══════════ -->
    <el-dialog
      v-model="showPublishDialog"
      title="发布古建动态"
      width="680px"
      :close-on-click-modal="false"
      class="cs-dialog"
      @closed="resetPublishForm"
    >
      <el-form :model="publishForm" label-position="top" class="cs-form">
        <el-form-item label="标题">
          <el-input
            v-model="publishForm.title"
            placeholder="给你的古建记忆起个名字"
            maxlength="128"
            show-word-limit
            size="large"
          />
        </el-form-item>

        <el-form-item label="封面图">
          <ImageUploader
            v-model="publishForm.preview2dPath"
            placeholder="上传封面图（可选）"
            :show-url-input="true"
            sub-dir="covers"
          />
        </el-form-item>

        <el-form-item label="正文">
          <div v-if="showPublishDialog" class="cs-editor">
            <QuillEditor
              v-model:content="publishForm.content"
              content-type="html"
              :options="editorOptions"
              class="cs-quill"
            />
          </div>
          <div class="cs-editor-foot">
            <span>已输入 {{ contentLength }} / 5000 字</span>
          </div>
        </el-form-item>

        <el-form-item label="标签">
          <div class="v4-fieldset cs-tags">
            <span class="lab">最多 5 个 · 用于社区检索与归档</span>
            <div class="cs-tagrow">
              <el-tag
                v-for="tag in publishForm.tags"
                :key="tag"
                closable
                type="primary"
                @close="removeTag(tag)"
              >
                {{ tag }}
              </el-tag>
              <el-input
                v-if="showTagInput"
                ref="tagInputRef"
                v-model="newTag"
                size="small"
                class="cs-taginput"
                placeholder="输入标签"
                @keyup.enter="addTag"
                @blur="addTag"
              />
              <button v-else class="v4-btn v4-btn--ghost v4-btn--sm" type="button" @click="showTagInput = true">
                <el-icon :size="14"><Plus /></el-icon>
                添加标签
              </button>
            </div>
            <div class="cs-suggest">
              <span class="cs-suggest-lab">推荐：</span>
              <button
                v-for="tag in suggestedTags"
                :key="tag"
                class="v4-chip cs-suggest-chip"
                type="button"
                :aria-label="`添加标签 ${tag}`"
                @click="addSuggestedTag(tag)"
              >
                {{ tag }}
              </button>
            </div>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <button class="v4-btn v4-btn--ghost" type="button" @click="showPublishDialog = false">
          取消
        </button>
        <button
          class="v4-btn v4-btn--gold"
          type="button"
          :disabled="publishing"
          @click="publishPost"
        >
          {{ publishing ? '发布中…' : '发布动态' }}
        </button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, defineAsyncComponent } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Edit, Search, Close, Plus, MagicStick, Loading } from '@element-plus/icons-vue'
import { communityApi } from '@/api/community'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import ImageUploader from '@/components/ImageUploader.vue'
import PostCard from '@/components/PostCard.vue'
import { recordsFallback } from '@/utils/pagination'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/* 懒加载富文本编辑器，仅在打开发布弹窗时下载 */
const QuillEditor = defineAsyncComponent(async () => {
  await import('@vueup/vue-quill/dist/vue-quill.snow.css')
  return import('@vueup/vue-quill')
})

/* ═══ 列表数据 ═══ */
const posts = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 12
const loading = ref(false)
const error = ref('')
const errorDetail = ref('')

/* ═══ 检索：唯一来源是 route.query.q（顶栏与本页输入框都只写它） ═══ */
const keyword = ref(route.query.q ? String(route.query.q) : '')
const activeKeyword = computed(() => (route.query.q ? String(route.query.q).trim() : ''))

/* 后端 /v1/posts 不支持关键词参数（决策 3），因此只对已加载的当前页做前端筛选。
   仅使用 PostBriefResponse 真实存在的字段：title / authorNickname / tags。 */
const visiblePosts = computed(() => {
  const k = activeKeyword.value.toLowerCase()
  if (!k) return posts.value
  return posts.value.filter((p) => {
    const hay = [p.title, p.authorNickname, p.tags].filter(Boolean).join(' ').toLowerCase()
    return hay.includes(k)
  })
})

watch(
  () => route.query.q,
  (q) => { keyword.value = q ? String(q) : '' }
)

function submitFilter() {
  const q = keyword.value.trim()
  router.replace({ path: '/community', query: q ? { q } : {} })
}

function clearKeyword() {
  keyword.value = ''
  router.replace({ path: '/community' })
}

/* ═══ 加载（自持 try/catch，useAsyncAction 会吞掉错误，拿不到错误态） ═══ */
async function load() {
  loading.value = true
  error.value = ''
  errorDetail.value = ''
  try {
    const res = await communityApi.getPosts({ page: currentPage.value, size: pageSize })
    posts.value = recordsFallback(res.data)
    total.value = res.data?.total || 0
  } catch (e) {
    posts.value = []
    total.value = 0
    const status = e?.response?.status
    if (status === 401 || status === 403) {
      error.value = '暂时无法查看公开档案'
      errorDetail.value = '当前账号没有读取该列表的权限，请重新登录后再试。'
    } else if (status) {
      error.value = '档案列表加载失败'
      errorDetail.value = `请求未能完成（HTTP ${status}），请稍后重试。`
    } else {
      error.value = '档案列表加载失败'
      errorDetail.value = '网络或服务暂时不可用，请检查网络后重试。'
    }
  } finally {
    loading.value = false
  }
}

function onPageChange(p) {
  currentPage.value = p
  load()
}

/* ═══ 发帖 ═══ */
const showPublishDialog = ref(false)
const publishing = ref(false)
const publishForm = ref({
  title: '',
  preview2dPath: '',
  content: '',
  tags: []
})

const showTagInput = ref(false)
const newTag = ref('')
const tagInputRef = ref(null)
const suggestedTags = ['唐代', '宋代', '明代', '清代', '斗栱', '歇山顶', '庑殿顶', '悬山顶', '佛光寺', '故宫', '园林', '民居']

const editorOptions = {
  placeholder: '讲述你与这座建筑的相遇...',
  modules: {
    toolbar: [
      ['bold', 'italic', 'underline', 'strike'],
      ['blockquote', 'code-block'],
      [{ header: 1 }, { header: 2 }],
      [{ list: 'ordered' }, { list: 'bullet' }],
      [{ indent: '-1' }, { indent: '+1' }],
      ['link', 'image'],
      ['clean']
    ]
  },
  theme: 'snow'
}

const contentLength = computed(() => publishForm.value.content.replace(/<[^>]*>/g, '').length)

function openPublish() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('登录后即可发布你的数字档案')
    goLogin()
    return
  }
  showPublishDialog.value = true
}

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
  publishForm.value.tags = publishForm.value.tags.filter((t) => t !== tag)
}

function addSuggestedTag(tag) {
  if (publishForm.value.tags.includes(tag)) return
  if (publishForm.value.tags.length >= 5) {
    ElMessage.warning('最多添加 5 个标签')
    return
  }
  publishForm.value.tags.push(tag)
}

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
    currentPage.value = 1
    load()
  } catch {
    /* 响应拦截器已统一提示 */
  } finally {
    publishing.value = false
  }
}

function resetPublishForm() {
  publishForm.value = { title: '', preview2dPath: '', content: '', tags: [] }
  showTagInput.value = false
  newTag.value = ''
}

/* ═══ 导航 ═══ */
function goLogin() {
  router.push('/login')
}

function goHuanZhu() {
  router.push('/huanzhu')
}

onMounted(load)
</script>

<style scoped>
/* ═══ 工具行 ═══ */
.cs-tools {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  min-width: 0;
}

.cs-search {
  width: 260px;
  max-width: 100%;
  cursor: text;
}

.cs-search-input {
  flex: 1;
  min-width: 0;
  height: 100%;
  background: none;
  border: none;
  outline: none;
  font: inherit;
  font-size: 13px;
  color: var(--color-text-main);
}

.cs-search-input::placeholder { color: var(--color-text-faint); }
.cs-search-input::-webkit-search-cancel-button { display: none; }

.cs-search-clear {
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  width: 20px;
  height: 20px;
  padding: 0;
  border: none;
  border-radius: var(--radius-full);
  background: var(--color-surface-hover);
  color: var(--color-text-sub);
  cursor: pointer;
}

.cs-search-clear:hover { color: var(--color-accent-text); }

/* ═══ 筛选声明 ═══ */
.cs-filterrow {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: var(--spacing-md);
}

.cs-filterpill { cursor: pointer; }
.cs-filterpill:hover { border-color: var(--color-border-gold); }

.cs-filternote {
  font-size: var(--font-size-meta);
  color: var(--color-text-faint);
  line-height: 1.7;
}

/* ═══ 发帖入口面板 ═══ */
.cs-compose { margin-bottom: var(--spacing-md); }

.cs-entry-btn {
  display: block;
  width: 100%;
  min-height: 62px;
  padding: 0;
  text-align: left;
  background: none;
  border: none;
  font-family: inherit;
  font-size: 13px;
  line-height: 1.85;
  color: var(--color-text-ghost);
  cursor: text;
}

.cs-entry-btn:hover { color: var(--color-text-muted); }

.cs-login-tip {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: var(--spacing-md);
}

.cs-login-tip > b {
  display: inline;
  margin-bottom: 0;
}

/* ═══ 信息流（保留信息流形态） ═══ */
.cs-feed {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(300px, 100%), 1fr));
  gap: var(--spacing-lg);
  align-items: start;
}

/* ═══ 骨架 ═══ */
.cs-skel {
  border-radius: var(--radius-2xl);
  overflow: hidden;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
}

.cs-skel-img { width: 100%; height: 158px; }
.cs-skel-bd {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: var(--spacing-md);
}
.cs-skel-t { width: 62%; height: 18px; }
.cs-skel-m { width: 40%; }

.cs-skel-tip {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
  font-size: var(--font-size-caption);
  color: var(--color-text-muted);
}

/* ═══ 空态文案 ═══ */
.cs-empty-t {
  display: block;
  margin-bottom: 6px;
  font-family: var(--font-family-serif);
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-main);
}

.cs-empty-d {
  max-width: 460px;
  margin: 0 auto;
  font-size: var(--font-size-caption);
  line-height: 1.8;
  color: var(--color-text-muted);
}

.cs-empty-acts {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
  margin-top: var(--spacing-md);
}

/* ═══ 分页 ═══ */
.cs-pagination {
  display: flex;
  justify-content: center;
  margin-top: var(--spacing-xl);
}

/* ═══ 发帖对话框 ═══ */
.cs-form {
  max-height: 60vh;
  overflow-y: auto;
  padding-right: var(--spacing-sm);
}

/* el-dialog 的 class 会落到 .el-dialog 元素本身（Element Plus 透传 $attrs），
   两种落点都覆盖，避免样式静默失效 */
.cs-dialog,
.cs-dialog :deep(.el-dialog) { border-radius: var(--radius-xl); }
.cs-dialog :deep(.el-dialog__header) {
  padding: var(--spacing-lg) var(--spacing-xl);
  border-bottom: 1px solid var(--color-border);
  margin: 0;
}
.cs-dialog :deep(.el-dialog__body) { padding: var(--spacing-xl); }
.cs-dialog :deep(.el-dialog__footer) {
  padding: var(--spacing-md) var(--spacing-xl) var(--spacing-lg);
  border-top: 1px solid var(--color-border);
}

.cs-editor {
  width: 100%;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--color-border);
}

.cs-editor :deep(.ql-toolbar) {
  border: none;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-subtle);
}

.cs-editor :deep(.ql-container) {
  border: none;
  font-family: var(--font-family-base);
  font-size: 14px;
}

.cs-editor :deep(.ql-editor) {
  min-height: 200px;
  padding: var(--spacing-md);
}

.cs-editor :deep(.ql-editor.ql-blank::before) {
  color: var(--color-text-muted);
  font-style: normal;
}

.cs-editor-foot {
  display: flex;
  justify-content: flex-end;
  width: 100%;
  margin-top: var(--spacing-xs);
  font-size: var(--font-size-caption);
  color: var(--color-text-muted);
}

.cs-tags { width: 100%; }

.cs-tagrow {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
  align-items: center;
  min-height: 48px;
  padding: var(--spacing-sm);
  border-radius: var(--radius-md);
  background: var(--color-bg-subtle);
}

.cs-taginput { width: 140px; }

.cs-suggest {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-xs);
  align-items: center;
  margin-top: var(--spacing-sm);
}

.cs-suggest-lab {
  font-size: var(--font-size-caption);
  color: var(--color-text-muted);
}

.cs-suggest-chip { height: 26px; padding: 0 12px; font-size: var(--font-size-meta); }

/* ═══ 移动端（全局仅 900px 断点） ═══ */
@media (max-width: 900px) {
  .cs-feed { grid-template-columns: 1fr; gap: var(--spacing-md); }
  .cs-search { width: 100%; flex: 1 1 100%; }
  .cs-tools { width: 100%; }
  .cs-dialog,
  .cs-dialog :deep(.el-dialog) {
    width: calc(100% - 32px) !important;
    margin: 0 auto;
  }
}
</style>
