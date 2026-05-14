<template>
  <el-container class="app-shell">
    <!-- 顶部导航栏 -->
    <el-header class="app-header">
      <div class="header-left">
        <router-link to="/" class="logo-link">
          <span class="logo-icon">&#x2302;</span>
          <span class="logo-text">智观·古建</span>
        </router-link>
      </div>

      <div class="header-nav">
        <router-link to="/home" class="nav-item" :class="{ active: route.path === '/home' }">
          <el-icon><HomeFilled /></el-icon>
          <span>社区</span>
        </router-link>
        <router-link to="/huanzhu" class="nav-item" :class="{ active: route.path === '/huanzhu' }">
          <el-icon><MagicStick /></el-icon>
          <span>幻筑</span>
        </router-link>
        <router-link to="/zhixi" class="nav-item" :class="{ active: route.path === '/zhixi' }">
          <el-icon><Search /></el-icon>
          <span>智析</span>
        </router-link>
      </div>

      <div class="header-right">
        <!-- 通知铃铛 -->
        <el-badge :value="notifStore.unreadCount" :hidden="notifStore.unreadCount === 0" class="notif-badge">
          <el-button circle size="small" @click="goNotifications">
            <el-icon><Bell /></el-icon>
          </el-button>
        </el-badge>

        <template v-if="userStore.isLoggedIn">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar size="small" :icon="UserFilled" />
              <span class="username">{{ userStore.username || '用户' }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon> 个人中心
                </el-dropdown-item>
                <el-dropdown-item command="settings">
                  <el-icon><Setting /></el-icon> 设置
                </el-dropdown-item>
                <el-dropdown-item v-if="userStore.isAdmin" command="admin" divided>
                  <el-icon><Monitor /></el-icon> 审核工作台
                </el-dropdown-item>
                <el-dropdown-item command="notifications">
                  <el-icon><Bell /></el-icon> 通知中心
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <el-button v-else type="primary" size="small" @click="$router.push('/login')">
          登录
        </el-button>
      </div>
    </el-header>

    <!-- 主内容区 -->
    <el-main class="app-main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </el-main>

    <!-- 数字锦盒 Modal -->
    <el-dialog
      v-model="notifStore.showBrocade"
      title="&#x1F381; 数字锦盒已送达"
      width="520px"
      :close-on-click-modal="false"
      custom-class="brocade-modal"
      @closed="notifStore.closeBrocade()"
    >
      <div class="brocade-content">
        <p style="font-size:16px; line-height:1.8; margin-bottom:20px;">
          您的一键幻筑已生成完成<br/>
          <span style="color:var(--color-accent)">点击下方按钮，开启您的数字珍藏</span>
        </p>
        <el-button type="primary" size="large" @click="openBrocadeAsset">
          打开数字锦盒
        </el-button>
      </div>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { HomeFilled, MagicStick, Search, Bell, UserFilled, User, Setting, Monitor } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useNotificationStore } from '@/stores/notification'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const notifStore = useNotificationStore()

onMounted(() => {
  if (userStore.isLoggedIn) {
    notifStore.fetchUnreadCount()
  }
})

function goNotifications() {
  router.push('/notifications')
}

function openBrocadeAsset() {
  // 跳转到对应任务的资产展示页
  notifStore.showBrocade = false
  notifStore.fetchNotifications()
  router.push('/notifications')
}

function handleCommand(cmd) {
  if (cmd === 'logout') {
    userStore.logout()
  } else if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'settings') {
    router.push('/settings')
  } else if (cmd === 'admin') {
    router.push('/admin')
  } else if (cmd === 'notifications') {
    router.push('/notifications')
  }
}
</script>

<style scoped>
.app-shell { min-height: 100vh; }

/* 苍穹黑深色导航栏 — height: 64px 遵循4pt系统 */
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--spacing-xxl);
  height: var(--spacing-xxl);
  background: var(--color-header-bg);
  border-bottom: 1px solid rgba(181, 142, 54, 0.15);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left .logo-link {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  text-decoration: none;
  color: var(--color-secondary);
}

.logo-icon {
  font-size: 24px;
  color: var(--color-secondary);
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 2px;
  color: var(--color-secondary);
}

/* 导航菜单项 — 间距遵循spacing-xl (32px) */
.header-nav {
  display: flex;
  gap: var(--spacing-sm);
}

.nav-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-sm) var(--spacing-md);
  border-radius: var(--radius-md);
  text-decoration: none;
  color: var(--color-header-text);
  font-size: 14px;
  transition: all var(--transition-normal);
  position: relative;
}

.nav-item:hover {
  background: rgba(181, 142, 54, 0.12);
  color: var(--color-secondary);
}

.nav-item.active {
  background: rgba(181, 142, 54, 0.16);
  color: var(--color-secondary);
}

/* 选中态底部下划线 */
.nav-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: var(--spacing-md);
  right: var(--spacing-md);
  height: 2px;
  background: var(--color-primary);
  border-radius: 1px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.notif-badge :deep(.el-badge__content) {
  background-color: var(--color-primary);
}

/* 通知铃铛按钮 */
.header-right .el-button.is-circle {
  color: var(--color-header-text);
  background: transparent;
  border: 1px solid rgba(212, 197, 169, 0.2);
}

.header-right .el-button.is-circle:hover {
  background: rgba(181, 142, 54, 0.12);
  border-color: var(--color-secondary);
  color: var(--color-secondary);
}

.user-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  cursor: pointer;
  color: var(--color-header-text);
  padding: var(--spacing-xs) var(--spacing-sm);
  border-radius: var(--radius-md);
  transition: background var(--transition-fast);
}

.user-info:hover {
  background: rgba(181, 142, 54, 0.08);
}

.username { font-size: 14px; }

/* 登录按钮 */
.header-right > .el-button--primary {
  --el-button-bg-color: var(--color-primary);
  --el-button-border-color: var(--color-primary);
}

.app-main {
  padding: 0;
  min-height: calc(100vh - var(--spacing-xxl));
}

/* 移动端适配 */
@media (max-width: 768px) {
  .app-header {
    padding: 0 var(--spacing-md);
  }

  .header-nav .nav-item span {
    display: none;
  }

  .header-nav .nav-item {
    padding: var(--spacing-sm);
  }
}
</style>
