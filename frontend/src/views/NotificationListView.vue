<template>
  <div class="notif-page animate-fade-in">
    <div class="page-header">
      <h1>通知中心</h1>
      <el-button v-if="notifications.length" size="small" @click="markAllRead">全部已读</el-button>
    </div>

    <div v-loading="loading">
      <div v-if="notifications.length" class="notification-list glass-card">
        <div
          v-for="n in notifications"
          :key="n.id"
          class="notification-item"
          :class="{ unread: !n.read }"
          @click="handleClick(n)"
        >
          <div class="notif-icon">
            <el-icon size="20" :color="n.read ? 'var(--color-text-muted)' : 'var(--color-accent)'">
              <Present />
            </el-icon>
          </div>
          <div class="notif-body">
            <p class="notif-message">{{ n.message }}</p>
            <span class="notif-time">{{ n.createdAt }}</span>
          </div>
          <div v-if="!n.read" class="notif-dot"></div>
        </div>
      </div>
      <el-empty v-else-if="!loading" description="暂无通知" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Present } from '@element-plus/icons-vue'
import { useNotificationStore } from '@/stores/notification'
import { ElMessage } from 'element-plus'
import { useAsyncAction } from '@/composables/useAsyncAction'

const router = useRouter()
const notifStore = useNotificationStore()
const notifications = ref([])

const { loading, run: fetchNotifications } = useAsyncAction(async () => {
  await notifStore.fetchNotifications()
  notifications.value = notifStore.notifications || []
})

async function handleClick(n) {
  if (!n.read) {
    await notifStore.markAsRead(n.id)
    n.read = true
  }
  if (n.assetUrl) {
    router.push(n.assetUrl)
  }
}

async function markAllRead() {
  await notifStore.markAllRead()
  notifications.value.forEach(n => n.read = true)
  ElMessage.success('已全部标为已读')
}

onMounted(fetchNotifications)
</script>

<style scoped>
.notif-page {
  height: 100%;
  overflow-y: auto;
  padding: var(--spacing-xl);
}

.notif-page::-webkit-scrollbar { width: 6px; }
.notif-page::-webkit-scrollbar-track { background: transparent; }
.notif-page::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.1); border-radius: 3px; }

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-lg);
}

.page-header h1 {
  font-family: var(--font-family-serif);
  font-size: var(--font-size-title);
  color: var(--color-text-main);
}

.notification-list {
  padding: 0;
  overflow: hidden;
}

.notification-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-md) var(--spacing-lg);
  cursor: pointer;
  transition: background var(--transition-fast);
  border-bottom: 1px solid var(--color-border);
}

.notification-item:last-child { border-bottom: none; }
.notification-item:hover { background: var(--color-bg-subtle); }
.notification-item.unread { background: var(--color-accent-soft); }

.notif-body { flex: 1; }

.notif-message {
  font-size: 14px;
  color: var(--color-text-main);
  margin-bottom: 4px;
}

.notif-time {
  font-size: var(--font-size-caption);
  color: var(--color-text-muted);
}

.notif-dot {
  width: 8px;
  height: 8px;
  background: var(--color-accent);
  border-radius: 50%;
  flex-shrink: 0;
}
</style>
