<template>
  <aside class="sidebar">
    <!-- 品牌 -->
    <div class="sidebar-logo" @click="router.push('/home')">
      <div class="logo-icon-box">
        <el-icon :size="20"><HomeFilled /></el-icon>
      </div>
      <div class="logo-text">
        <h1 class="logo-name">智观·古建</h1>
        <p class="logo-sub">ARCHIVISION HERITAGE</p>
      </div>
    </div>

    <div class="sidebar-hr"></div>

    <!-- 主导航（5 项，与底栏共用同一份定义） -->
    <nav class="sidebar-nav" aria-label="主导航">
      <router-link
        v-for="item in navItems"
        :key="item.id"
        :to="item.path"
        class="nav-btn"
        :class="{ active: isActive(item.path) }"
        :aria-current="isActive(item.path) ? 'page' : undefined"
      >
        <span class="nav-icon-box" :class="{ active: isActive(item.path) }">
          <el-icon :size="18"><component :is="item.icon" /></el-icon>
        </span>
        <span class="nav-text">
          <span class="nav-label">{{ item.label }}</span>
          <span class="nav-desc">{{ item.desc }}</span>
        </span>
      </router-link>
    </nav>

    <div class="rail-spacer"></div>

    <!-- 底部用户区（点击开菜单） -->
    <div class="sidebar-footer" ref="menuRef">
      <UserMenu :open="menuOpen" placement="up" @close="menuOpen = false" />

      <button
        class="user-trigger"
        :class="{ 'is-open': menuOpen }"
        aria-haspopup="menu"
        :aria-expanded="menuOpen ? 'true' : 'false'"
        aria-controls="rail-user-menu"
        @click="menuOpen = !menuOpen"
      >
        <el-avatar :size="34" :icon="UserFilled" class="user-avatar" />
        <span class="user-text">
          <span class="user-name">
            {{ userStore.username || '访客' }}
            <el-icon v-if="userStore.isAdmin" :size="12" class="admin-star"><WarningFilled /></el-icon>
          </span>
          <span class="user-level">{{ userStore.isAdmin ? '平台管理员' : 'VGGT 认证匠人' }}</span>
        </span>
        <el-icon :size="14" class="user-arrow" :class="{ open: menuOpen }"><ArrowRight /></el-icon>
      </button>
    </div>
  </aside>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  HomeFilled, Box, MagicStick, Folder, User,
  UserFilled, WarningFilled, ArrowRight
} from '@element-plus/icons-vue'
import UserMenu from './UserMenu.vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/* ═══ 导航定义（5 项 · 决策 4）═══
   顺序即信息架构：先自我说明（营造志）→ 两个核心能力 → 我的沉淀 → 他人内容 */
const navItems = [
  { id: 'feed',      path: '/home',      icon: HomeFilled, label: '营造志',   desc: '平台与精选档案' },
  { id: 'zhixi',     path: '/zhixi',     icon: Box,        label: '古建智析', desc: 'VGGT 视觉解析' },
  { id: 'huanzhu',   path: '/huanzhu',   icon: MagicStick, label: '一键幻筑', desc: 'AI 3D 模型生成' },
  { id: 'archive',   path: '/archive',   icon: Folder,     label: '我的档案', desc: '解析 · 幻筑 · 收藏' },
  { id: 'community', path: '/community', icon: User,       label: '匠人社区', desc: '发现与交流' }
]

/** 详情页也要点亮其归属的一级入口（/community/post/1 → 匠人社区） */
function isActive(path) {
  if (route.path === path) return true
  if (path === '/home') return route.path === '/'
  return route.path.startsWith(`${path}/`)
}

/* ═══ 用户菜单：点外部 / Esc 关闭（侧栏与顶栏各自独立持有状态） ═══ */
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
/* ═══ 侧栏（V4：栏层实底 --color-bg-elev，与 bg-base 拉开分界）═══ */
.sidebar {
  width: var(--rail-w);
  min-width: var(--rail-w);
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 26px 12px 14px;
  background: var(--color-bg-elev);
  border-right: 1px solid var(--color-border-light);
  z-index: 20;
  transition: background-color var(--transition-theme), border-color var(--transition-theme);
}

/* 品牌 */
.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 12px 22px;
  cursor: pointer;
  user-select: none;
}

.logo-icon-box {
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: var(--color-accent-soft);
  border: 1px solid var(--color-border-gold);
  color: var(--color-accent-text);
}

.logo-text { min-width: 0; }

.logo-name {
  font-family: var(--font-family-serif);
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-main);
  letter-spacing: 0.5px;
}

.logo-sub {
  font-size: 9px;
  color: var(--color-text-ghost);
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.sidebar-hr {
  height: 1px;
  background: var(--color-border-light);
  margin: 0 12px 14px;
}

/* 导航 */
.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.nav-btn {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 12px;
  border-radius: 10px;
  text-decoration: none;
  color: var(--color-text-muted);
  font-size: 14px;
  font-weight: 500;
  transition: background-color var(--transition-normal), color var(--transition-normal);
}

.nav-btn:hover {
  background: var(--color-surface);
  color: var(--color-text-sub);
}

.nav-btn.active { color: var(--color-accent-text); }

/* 金色左侧标记（预览页 .nav.is-active::before） */
.nav-btn.active::before {
  content: '';
  position: absolute;
  left: -12px;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 18px;
  border-radius: 2px;
  background: var(--color-accent);
}

.nav-icon-box {
  width: 18px;
  height: 18px;
  flex: 0 0 18px;
  display: grid;
  place-items: center;
  color: currentColor;
}

.nav-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.nav-label {
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 1px;
  color: inherit;
  white-space: nowrap;
}

.nav-desc {
  font-size: 10px;
  color: var(--color-text-ghost);
  margin-top: 1px;
  white-space: nowrap;
}

.rail-spacer { flex: 1; }

/* ═══ 底部用户区 ═══ */
.sidebar-footer {
  margin-top: auto;
  position: relative;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 11px;
  width: 100%;
  padding: 11px 12px;
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  border: 1px solid transparent;
  cursor: pointer;
  text-align: left;
  transition: background-color var(--transition-normal), border-color var(--transition-normal);
}

.user-trigger:hover { border-color: var(--color-border-gold); }

.user-trigger.is-open {
  background: var(--color-surface-hover);
  border-color: var(--color-border-gold);
}

.user-avatar { flex: 0 0 34px; }

.user-text {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-main);
  display: flex;
  align-items: center;
  gap: 4px;
  line-height: 1.5;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.admin-star { color: var(--color-accent-text); flex: 0 0 auto; }

.user-level {
  font-size: 10px;
  color: var(--color-text-muted);
  display: block;
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-arrow {
  color: var(--color-text-muted);
  transition: transform var(--transition-normal);
  flex: 0 0 auto;
}

.user-arrow.open { transform: rotate(-90deg); }

/* ═══════════════════════════════════════════════════════════════
   移动端规则已收敛到 style.css 的全局 ≤900px 区块（D-9 单一来源）。
   本组件不再自带断点，避免与全局规则并存导致行为不可预测。
   ═══════════════════════════════════════════════════════════════ */
</style>
