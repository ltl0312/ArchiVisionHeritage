<template>
  <header class="topbar">
    <div class="tb-left">
      <div class="tb-title">{{ heading }}</div>
      <div class="tb-sub">{{ sub }}</div>
    </div>

    <div class="tb-right">
      <!-- 检索：后端帖子流暂不支持关键词查询（决策 3：不改后端），
           因此在匠人社区页面对「已加载的当前页」做前端筛选。 -->
      <form class="v4-search tb-search" role="search" @submit.prevent="submitSearch">
        <el-icon :size="16"><Search /></el-icon>
        <input
          v-model="keyword"
          type="search"
          class="tb-search-input"
          placeholder="在当前页档案中检索"
          aria-label="在当前页档案中检索"
        />
      </form>

      <button
        class="v4-iconbtn"
        aria-label="通知中心"
        @click="router.push('/notifications')"
      >
        <el-icon :size="18"><Bell /></el-icon>
        <span v-if="notifStore.unreadCount > 0" class="dot"></span>
      </button>

      <div class="tb-av" ref="menuRef">
        <button
          class="tb-av-btn"
          aria-haspopup="menu"
          aria-label="账户菜单"
          :aria-expanded="menuOpen ? 'true' : 'false'"
          aria-controls="topbar-user-menu"
          @click="menuOpen = !menuOpen"
        >
          <el-avatar :size="40" :icon="UserFilled" />
        </button>
        <UserMenu :open="menuOpen" placement="down" @close="menuOpen = false" />
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, Bell, UserFilled } from '@element-plus/icons-vue'
import UserMenu from './UserMenu.vue'
import { useNotificationStore } from '@/stores/notification'

const route = useRoute()
const router = useRouter()
const notifStore = useNotificationStore()

/* 标题随路由变化（meta.heading / meta.sub 在 router/index.js 单一来源定义） */
const heading = computed(() => route.meta.heading || route.meta.title || '智观·古建')
const sub = computed(() => route.meta.sub || '')

/* ═══ 检索 ═══ */
const keyword = ref('')

function submitSearch() {
  const q = keyword.value.trim()
  router.push({ path: '/community', query: q ? { q } : {} })
}

/* 从社区页离开时清空输入，避免残留旧的筛选词 */
router.afterEach((to) => {
  if (to.path !== '/community') keyword.value = ''
})

/* ═══ 账户菜单 ═══ */
const menuOpen = ref(false)
const menuRef = ref(null)

function onDocClick(e) {
  if (menuRef.value && !menuRef.value.contains(e.target)) menuOpen.value = false
}
function onKeydown(e) {
  if (e.key === 'Escape') menuOpen.value = false
}

onMounted(() => {
  document.addEventListener('mousedown', onDocClick)
  document.addEventListener('keydown', onKeydown)
})
onUnmounted(() => {
  document.removeEventListener('mousedown', onDocClick)
  document.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
.topbar {
  height: var(--topbar-h);
  flex: 0 0 var(--topbar-h);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-md);
  padding: 0 var(--spacing-xl);
  border-bottom: 1px solid var(--color-border-light);
  background: transparent;
  transition: border-color var(--transition-theme);
}

.tb-left { min-width: 0; }

.tb-title {
  font-family: var(--font-family-serif);
  font-size: 22px;
  font-weight: 600;
  line-height: 1.3;
  color: var(--color-text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tb-sub {
  font-size: 11px;
  color: var(--color-text-faint);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tb-right {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  flex: 0 0 auto;
}

/* 检索框：以原语 .v4-search 为底，内部换成真实 input */
.tb-search {
  cursor: text;
  color: var(--color-text-faint);
}

.tb-search-input {
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

.tb-search-input::placeholder { color: var(--color-text-faint); }

/* 去掉 type=search 的原生清除按钮，保持胶囊外观一致 */
.tb-search-input::-webkit-search-cancel-button { display: none; }

.tb-av {
  position: relative;
  display: flex;
  align-items: center;
}

.tb-av-btn {
  border-radius: 50%;
  display: block;
  line-height: 0;
  padding: 0;
  background: none;
  border: none;
  cursor: pointer;
}

/* 移动端：标题缩小、副标题与检索框隐藏（规则集中在本组件，仅此处使用） */
@media (max-width: 900px) {
  .topbar { padding: 0 var(--spacing-md); gap: 10px; }
  .tb-title { font-size: 19px; }
  .tb-sub { display: none; }
  .tb-search { display: none; }
  .tb-right { gap: 10px; }
}
</style>
