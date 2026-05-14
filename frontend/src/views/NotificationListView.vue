<template>
  <div class="notification-page">
    <div class="page-header">
      <h1>通知中心</h1>
      <el-button v-if="notifications.length" size="small" @click="markAllRead">全部已读</el-button>
    </div>

    <div v-loading="loading">
      <div v-if="notifications.length" class="notification-list">
        <div
          v-for="n in notifications"
          :key="n.id"
          class="notification-item"
          :class="{ unread: !n.read }"
          @click="handleClick(n)"
        >
          <div class="notif-icon">
            <el-icon size="20" :color="n.read ? '#999' : '#d73c37'">
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

      <el-empty v-else description="暂无通知">
        <template #image>
          <el-icon size="60" color="#ccc"><Bell /></el-icon>
        </template>
        <p style="color:var(--color-text-muted); font-size:13px;">
          当您的一键幻筑完成时，数字锦盒将在此送达
        </p>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Bell, Present } from '@element-plus/icons-vue'
import { notificationApi } from '@/api/notification'

const router = useRouter()
const notifications = ref([])
const loading = ref(false)

onMounted(async () => {
  await fetchNotifications()
})

async function fetchNotifications() {
  loading.value = true
  try {
    const res = await notificationApi.list()
    notifications.value = res.data || []
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function handleClick(n) {
  if (!n.read) {
    try {
      await notificationApi.markAsRead(n.id)
      n.read = true
    } catch { /* ignore */ }
  }
  // 跳转到社区首页
  router.push('/home')
}

async function markAllRead() {
  try {
    await notificationApi.markAllAsRead()
    notifications.value.forEach(n => n.read = true)
    ElMessage.success('已全部标为已读')
  } catch { /* ignore */ }
}
</script>

<style scoped>
.notification-page {
  max-width: 700px;
  margin: 0 auto;
  padding: var(--spacing-lg);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--spacing-lg);
}

.page-header h1 {
  font-size: var(--font-size-title);
  font-weight: 700;
  color: var(--color-text-main);
}

.notification-list {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-card);
}

.notification-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-md) var(--spacing-lg);
  cursor: pointer;
  transition: background var(--transition-fast);
  border-bottom: 1px solid #f0f0f0;
}

.notification-item:last-child { border-bottom: none; }
.notification-item:hover { background: #fafafa; }
.notification-item.unread { background: rgba(184, 38, 31, 0.03); }

.notif-body { flex: 1; }

.notif-message {
  font-size: 14px;
  color: var(--color-text-main);
  margin-bottom: var(--spacing-xs);
}

.notif-time {
  font-size: var(--font-size-caption);
  color: var(--color-text-muted);
}

.notif-dot {
  width: var(--spacing-sm);
  height: var(--spacing-sm);
  background: var(--color-primary);
  border-radius: 50%;
  flex-shrink: 0;
}
</style>
