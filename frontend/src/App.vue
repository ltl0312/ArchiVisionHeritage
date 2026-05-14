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
                <el-dropdown-item command="notifications">通知中心</el-dropdown-item>
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
import { HomeFilled, MagicStick, Search, Bell, UserFilled } from '@element-plus/icons-vue'
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
  } else if (cmd === 'notifications') {
    router.push('/notifications')
  }
}
</script>

<style scoped>
.app-shell { min-height: 100vh; }

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: linear-gradient(135deg, #2c1810 0%, #4a2c17 100%);
  border-bottom: 1px solid rgba(223,188,94,0.2);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-left .logo-link {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  color: var(--color-accent);
}

.logo-icon { font-size: 24px; }

.logo-text {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 2px;
}

.header-nav {
  display: flex;
  gap: 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
  border-radius: var(--radius-md);
  text-decoration: none;
  color: #d4c5a9;
  font-size: 14px;
  transition: all 0.3s;
}

.nav-item:hover { background: rgba(223,188,94,0.15); color: var(--color-accent); }
.nav-item.active { background: rgba(223,188,94,0.2); color: var(--color-accent); }

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.notif-badge :deep(.el-badge__content) {
  background-color: var(--color-primary);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #d4c5a9;
}

.username { font-size: 14px; }

.app-main {
  padding: 0;
  min-height: calc(100vh - 60px);
}
</style>
