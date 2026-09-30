<template>
  <div class="app-root" :class="{ dark: themeStore.isDark }">
    <!-- 环境光晕（装饰层，pointer-events: none） -->
    <div class="ambient-glow-top"></div>
    <div class="ambient-glow-bottom"></div>

    <!-- 侧栏导航（5 项一级入口） -->
    <SideNav />

    <!-- 主内容区：顶栏 + 视图宿主 -->
    <main class="main-content">
      <TopBar />

      <!-- 视图宿主：唯一的滚动容器；界画测绘底纹挂在这一静态层上
           （原为 body::before 全屏固定层，下移可减少一个固定层） -->
      <div class="view-host v4-mesh">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>

    <!-- 数字锦盒 Modal -->
    <BrocadeDialog />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import SideNav from '@/components/Layout/SideNav.vue'
import TopBar from '@/components/Layout/TopBar.vue'
import BrocadeDialog from '@/components/Layout/BrocadeDialog.vue'
import { useUserStore } from '@/stores/user'
import { useNotificationStore } from '@/stores/notification'
import { useThemeStore } from '@/stores/theme'

const userStore = useUserStore()
const notifStore = useNotificationStore()
const themeStore = useThemeStore()

/* ═══ 初始化：主题 + 信息密度（都在 documentElement 上生效，需在首帧前应用） ═══ */
themeStore.initTheme()
themeStore.initDensity()

onMounted(() => {
  if (userStore.isLoggedIn) {
    userStore.parseToken()
    notifStore.fetchUnreadCount()
  }
})
</script>

<style>
/* 非 scoped：视图宿主是全局滚动容器，供全局移动端规则覆盖 */
.view-host {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  position: relative;
  z-index: 10;
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
  min-width: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
  z-index: 10;
  background: transparent;
}
</style>
