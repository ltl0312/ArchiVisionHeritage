<template>
  <aside class="sidebar">
    <!-- Logo -->
    <div class="sidebar-logo" @click="router.push('/home')">
      <div class="logo-icon-box">
        <span class="logo-glyph">&#x2302;</span>
      </div>
      <div>
        <h1 class="logo-name">智观·古建</h1>
        <p class="logo-sub">ZHIGUAN HERITAGE</p>
      </div>
    </div>

    <!-- 主导航 -->
    <nav class="sidebar-nav">
      <router-link
        v-for="item in navItems"
        :key="item.id"
        :to="item.path"
        class="nav-btn"
        :class="{ active: isActive(item.path) }"
      >
        <div class="nav-icon-box" :class="{ active: isActive(item.path) }">
          <el-icon :size="20"><component :is="item.icon" /></el-icon>
        </div>
        <div class="nav-text">
          <span class="nav-label">{{ item.label }}</span>
          <span class="nav-desc">{{ item.desc }}</span>
        </div>
      </router-link>
    </nav>

    <!-- 底部用户区 -->
    <div class="sidebar-footer" ref="menuRef">
      <!-- 弹出菜单 -->
      <transition name="menu-pop">
        <div v-if="menuOpen" class="user-menu">
          <button class="menu-item" @click="goAndClose('/profile')">
            <el-icon :size="16"><User /></el-icon> 个人信息
          </button>
          <button class="menu-item" @click="goAndClose('/settings')">
            <el-icon :size="16"><Setting /></el-icon> 设置
          </button>
          <button class="menu-item" @click="goAndClose('/notifications')">
            <el-icon :size="16"><Bell /></el-icon>
            <!-- 未读徽标：接线 unreadCount（原为死数据，仅拉取不展示） -->
            <el-badge :value="notifStore.unreadCount" :hidden="notifStore.unreadCount === 0" :max="99">
              通知中心
            </el-badge>
          </button>

          <div class="menu-divider"></div>

          <button class="menu-item menu-item-theme" @click="themeStore.toggleTheme">
            <div class="menu-item-left">
              <el-icon :size="16">
                <Sunny v-if="!themeStore.isDark" />
                <Moon v-else />
              </el-icon>
              昼夜流转
            </div>
            <span class="theme-tag">{{ themeStore.isDark ? '夜影' : '晨光' }}</span>
          </button>

          <template v-if="userStore.isAdmin">
            <div class="menu-divider"></div>
            <button class="menu-item menu-item-admin" @click="goAndClose('/admin')">
              <el-icon :size="16"><WarningFilled /></el-icon> 管理控制台
            </button>
          </template>

          <div class="menu-divider"></div>
          <button class="menu-item menu-item-logout" @click="handleLogout">
            <el-icon :size="16"><SwitchButton /></el-icon> 安全退出
          </button>
        </div>
      </transition>

      <!-- 触发区 -->
      <div class="user-trigger" @click="menuOpen = !menuOpen" :class="{ active: menuOpen }">
        <el-avatar :size="40" :icon="UserFilled" />
        <div class="user-text">
          <span class="user-name">
            {{ userStore.username || '用户' }}
            <el-icon v-if="userStore.isAdmin" :size="12" class="admin-star"><WarningFilled /></el-icon>
          </span>
          <span class="user-level">Lv.4 营造学徒</span>
        </div>
        <el-icon :size="16" class="user-arrow" :class="{ open: menuOpen }"><ArrowRight /></el-icon>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  HomeFilled, MagicStick, Search, Bell, UserFilled,
  User, Setting, Sunny, Moon, WarningFilled, SwitchButton, ArrowRight
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useNotificationStore } from '@/stores/notification'
import { useThemeStore } from '@/stores/theme'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const notifStore = useNotificationStore()
const themeStore = useThemeStore()

/* ═══ 导航定义 ═══ */
const navItems = [
  { id: 'community', path: '/home', icon: HomeFilled, label: '文化游廊', desc: '发现与分享' },
  { id: 'zhixi',     path: '/zhixi',   icon: Search,      label: '古建智析', desc: 'VGGT 视觉解析' },
  { id: 'huanzhu',   path: '/huanzhu', icon: MagicStick,  label: '一键幻筑', desc: 'AI 3D 模型生成' },
]

function isActive(path) {
  return route.path === path || (path === '/home' && route.path === '/')
}

/* ═══ 用户菜单 ═══ */
const menuOpen = ref(false)
const menuRef = ref(null)

function goAndClose(path) {
  menuOpen.value = false
  router.push(path)
}

function handleLogout() {
  menuOpen.value = false
  userStore.logout()
}

function onDocClick(e) {
  if (menuRef.value && !menuRef.value.contains(e.target)) {
    menuOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('mousedown', onDocClick)
})
onUnmounted(() => {
  document.removeEventListener('mousedown', onDocClick)
})
</script>

<style scoped>
/* ═══ 侧边栏（原 App.vue scoped 侧边栏块逐字搬移）═══ */
.sidebar {
  width: 260px;
  min-width: 260px;
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: var(--spacing-lg);
  background: var(--color-surface);
  border-right: 1px solid var(--color-border);
  z-index: 20;
  transition: background-color var(--transition-theme), border-color var(--transition-theme);
}

[data-theme="dark"] .sidebar {
  border-right-color: rgba(255, 255, 255, 0.06);
}

/* Logo */
.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 4px;
  margin-bottom: 40px;
  cursor: pointer;
  user-select: none;
}

.logo-icon-box {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, var(--color-accent), var(--color-rose));
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(225, 29, 72, 0.2);
}

.logo-glyph {
  font-size: 22px;
  color: #FFF;
  line-height: 1;
}

.logo-name {
  font-family: var(--font-family-serif);
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-main);
  letter-spacing: 2px;
}

.logo-sub {
  font-size: 10px;
  color: var(--color-text-muted);
  letter-spacing: 2px;
  text-transform: uppercase;
  margin-top: 2px;
}

/* 导航 */
.sidebar-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-btn {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: var(--radius-xl);
  text-decoration: none;
  transition: all var(--transition-normal);
  position: relative;
}

.nav-btn:hover {
  background: var(--color-bg-subtle);
}

.nav-btn.active {
  background: var(--color-surface-hover);
  box-shadow: var(--shadow-card);
  border: 1px solid var(--color-border);
}

[data-theme="dark"] .nav-btn.active {
  border-color: rgba(255, 255, 255, 0.06);
}

.nav-icon-box {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-subtle);
  color: var(--color-text-sub);
  transition: all var(--transition-normal);
  flex-shrink: 0;
}

.nav-btn:hover .nav-icon-box {
  background: rgba(0, 0, 0, 0.08);
}

.nav-icon-box.active {
  background: var(--color-accent-soft);
  color: var(--color-accent);
}

[data-theme="dark"] .nav-icon-box.active {
  color: var(--color-accent-light);
}

.nav-text {
  display: flex;
  flex-direction: column;
}

.nav-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-main);
  letter-spacing: 1px;
}

.nav-btn.active .nav-label {
  color: var(--color-accent);
}

.nav-desc {
  font-size: 10px;
  color: var(--color-text-muted);
  margin-top: 1px;
}

/* ═══ 底部用户区 ═══ */
.sidebar-footer {
  margin-top: auto;
  position: relative;
}

/* 用户弹出菜单 */
.user-menu {
  position: absolute;
  bottom: 100%;
  left: 0;
  right: 0;
  margin-bottom: 12px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  padding: 6px;
  box-shadow: var(--shadow-modal);
  z-index: 30;
}

[data-theme="dark"] .user-menu {
  border-color: rgba(255, 255, 255, 0.06);
}

.menu-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: none;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-text-main);
  font-size: 13px;
  cursor: pointer;
  transition: background var(--transition-fast);
}

.menu-item:hover {
  background: var(--color-bg-subtle);
}

.menu-item-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.menu-item-theme {
  justify-content: space-between;
}

.theme-tag {
  font-size: 11px;
  color: var(--color-text-muted);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
}

.menu-item-admin {
  color: var(--color-rose);
}

.menu-item-admin:hover {
  background: var(--color-rose-soft);
}

.menu-item-logout {
  color: var(--color-text-muted);
}

.menu-divider {
  height: 1px;
  background: var(--color-border);
  margin: 4px 0;
}

/* 菜单弹出动画 */
.menu-pop-enter-active {
  transition: all 0.25s ease;
}
.menu-pop-leave-active {
  transition: all 0.2s ease;
}
.menu-pop-enter-from {
  opacity: 0;
  transform: translateY(8px) scale(0.95);
}
.menu-pop-leave-to {
  opacity: 0;
  transform: translateY(8px) scale(0.95);
}

/* 用户触发区 */
.user-trigger {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border-top: 1px solid var(--color-border);
  cursor: pointer;
  transition: opacity var(--transition-fast);
  border-radius: var(--radius-md);
}

.user-trigger:hover {
  background: var(--color-bg-subtle);
}

.user-trigger.active {
  opacity: 0.5;
}

.user-text {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-main);
  display: flex;
  align-items: center;
  gap: 4px;
}

.admin-star {
  color: var(--color-accent);
}

.user-level {
  font-size: 11px;
  color: var(--color-text-muted);
  display: block;
}

.user-arrow {
  color: var(--color-text-muted);
  transition: transform var(--transition-normal);
  flex-shrink: 0;
}

.user-arrow.open {
  transform: rotate(-90deg);
}

/* 移动端适配 */
@media (max-width: 768px) {
  .sidebar {
    width: 72px;
    min-width: 72px;
    padding: var(--spacing-md) var(--spacing-sm);
  }

  .sidebar-logo {
    justify-content: center;
    margin-bottom: 24px;
  }

  .logo-name,
  .logo-sub,
  .nav-text,
  .user-text,
  .user-arrow {
    display: none;
  }

  .nav-btn {
    justify-content: center;
    padding: 10px;
  }

  .user-trigger {
    justify-content: center;
    padding: 10px 0;
  }
}
</style>
