<template>
  <div class="app-root" :class="{ 'dark': themeStore.isDark }">
    <!-- 环境光晕 -->
    <div class="ambient-glow-top"></div>
    <div class="ambient-glow-bottom"></div>

    <!-- 侧边栏 -->
    <SideNav />

    <!-- 主内容区 -->
    <main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="fade">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <!-- 数字锦盒 Modal -->
    <BrocadeDialog />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import SideNav from '@/components/Layout/SideNav.vue'
import BrocadeDialog from '@/components/Layout/BrocadeDialog.vue'
import { useUserStore } from '@/stores/user'
import { useNotificationStore } from '@/stores/notification'
import { useThemeStore } from '@/stores/theme'

const userStore = useUserStore()
const notifStore = useNotificationStore()
const themeStore = useThemeStore()

/* ═══ 初始化 ═══ */
onMounted(() => {
  themeStore.initTheme()
  if (userStore.isLoggedIn) {
    userStore.parseToken()
    notifStore.fetchUnreadCount()
  }
})
</script>

<style>
/* 非 scoped — 全局滚动条类应用于主内容区 */
.main-content {
  overflow-y: auto;
}
</style>

<style scoped>
/* ═══ 根容器 ═══ */
.app-root {
  display: flex;
  height: 100vh;
  width: 100%;
  overflow: hidden;
  font-family: var(--font-family-base);
  background-color: var(--color-bg-base);
  transition: background-color var(--transition-theme), color var(--transition-theme);
  position: relative;
}

/* ═══ 主内容区 ═══ */
.main-content {
  flex: 1;
  height: 100vh;
  position: relative;
  z-index: 10;
  background: transparent;
}
</style>
