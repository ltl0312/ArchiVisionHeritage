<template>
  <transition :name="placement === 'up' ? 'menu-up' : 'menu-down'">
    <div
      v-if="open"
      class="user-menu"
      :class="placement === 'up' ? 'user-menu--up' : 'user-menu--down'"
      role="menu"
      :aria-label="'账户菜单'"
    >
      <div class="menu-head">
        <b>{{ userStore.username || '未登录' }}</b>
        <span>{{ subtitle }}</span>
      </div>

      <button class="menu-item" role="menuitem" @click="go('/archive')">
        <el-icon :size="16"><User /></el-icon> 个人信息
      </button>
      <button class="menu-item" role="menuitem" @click="go('/settings')">
        <el-icon :size="16"><Setting /></el-icon> 设置
      </button>
      <button class="menu-item" role="menuitem" @click="go('/notifications')">
        <el-icon :size="16"><Bell /></el-icon> 通知中心
        <span v-if="notifStore.unreadCount > 0" class="badge">{{ badgeText }}</span>
      </button>

      <div class="menu-sep"></div>

      <button class="menu-item" role="menuitem" @click="onToggleTheme">
        <el-icon :size="16">
          <Sunny v-if="themeStore.isDark" />
          <Moon v-else />
        </el-icon>
        昼夜流转
        <span class="tail">{{ themeStore.isDark ? '夜影' : '晨光' }}</span>
      </button>

      <template v-if="userStore.isAdmin">
        <div class="menu-sep"></div>
        <button class="menu-item menu-item--admin" role="menuitem" @click="go('/admin')">
          <el-icon :size="16"><WarningFilled /></el-icon> 管理控制台
        </button>
      </template>

      <div class="menu-sep"></div>
      <button class="menu-item menu-item--danger" role="menuitem" @click="onLogout">
        <el-icon :size="16"><SwitchButton /></el-icon> 安全退出
      </button>
    </div>
  </transition>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  User, Setting, Bell, Sunny, Moon, WarningFilled, SwitchButton
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useNotificationStore } from '@/stores/notification'
import { useThemeStore } from '@/stores/theme'

/**
 * 账户菜单（侧栏用户条 / 顶栏头像 共用，D-11 去重）。
 * 打开与关闭由父级持有（点外部 / Esc 关闭由父级统一监听）。
 */
const props = defineProps({
  open: { type: Boolean, default: false },
  /** 'up' = 侧栏向上弹出；'down' = 顶栏向下弹出 */
  placement: { type: String, default: 'up' }
})

const emit = defineEmits(['close'])

const router = useRouter()
const userStore = useUserStore()
const notifStore = useNotificationStore()
const themeStore = useThemeStore()

const subtitle = computed(() => {
  const role = userStore.isAdmin ? '管理员' : '匠人'
  return `${userStore.username || '访客'} · ${role}`
})

const badgeText = computed(() =>
  notifStore.unreadCount > 99 ? '99+' : String(notifStore.unreadCount)
)

function go(path) {
  emit('close')
  router.push(path)
}

function onToggleTheme() {
  themeStore.toggleTheme()
}

function onLogout() {
  emit('close')
  userStore.logout()
}

/* 供父级判断点击是否落在菜单内部（点外部关闭） */
defineExpose({ placement: props.placement })
</script>

<style scoped>
.user-menu {
  position: absolute;
  z-index: 80;
  min-width: 212px;
  padding: 6px;
  background: var(--color-surface-float);
  border: 1px solid var(--color-border-gold);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-modal);
}

/* 侧栏：自用户条上方展开 */
.user-menu--up {
  left: 0;
  right: 0;
  bottom: calc(100% + 10px);
}

/* 顶栏：自头像下方右对齐展开 */
.user-menu--down {
  right: 0;
  top: calc(100% + 10px);
}

.menu-head {
  padding: 8px 11px 10px;
  border-bottom: 1px solid var(--color-border);
  margin-bottom: 5px;
}
.menu-head > b {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-main);
}
.menu-head > span {
  font-size: 10px;
  color: var(--color-text-faint);
}

.menu-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 11px;
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--color-text-main);
  text-align: left;
  background: none;
  border: none;
  cursor: pointer;
  transition: background-color var(--transition-fast);
}
.menu-item:hover { background: var(--color-surface-hover); }

.menu-item .badge {
  margin-left: auto;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: var(--color-rose);
  color: var(--color-on-rose);
  font-size: 10px;
  font-weight: 600;
  display: grid;
  place-items: center;
}

.menu-item .tail {
  margin-left: auto;
  font-size: 11px;
  color: var(--color-text-faint);
}

.menu-item--admin { color: var(--color-rose-text); }
.menu-item--danger { color: var(--color-text-muted); }

.menu-sep {
  height: 1px;
  background: var(--color-border);
  margin: 5px 2px;
}

/* 弹出动画 */
.menu-up-enter-active,
.menu-down-enter-active { transition: opacity 0.18s ease, transform 0.18s ease; }
.menu-up-leave-active,
.menu-down-leave-active { transition: opacity 0.16s ease, transform 0.16s ease; }
.menu-up-enter-from,
.menu-up-leave-to { opacity: 0; transform: translateY(8px) scale(0.97); }
.menu-down-enter-from,
.menu-down-leave-to { opacity: 0; transform: translateY(-8px) scale(0.97); }
</style>
