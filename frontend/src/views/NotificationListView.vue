<template>
  <div class="notif-page animate-fade-in">
    <div class="page-header">
      <h1>通知中心</h1>
      <el-button v-if="notifications.length" size="small" @click="markAllRead">全部已读</el-button>
    </div>

    <div v-loading="loading">
      <div v-if="notifications.length" class="notification-list glass-card">
        <NotificationItem
          v-for="n in notifications"
          :key="n.id"
          :notification="n"
          @click="handleClick"
        />
      </div>
      <el-empty v-else-if="!loading" description="暂无通知" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useNotificationStore } from '@/stores/notification'
import { ElMessage } from 'element-plus'
import { useAsyncAction } from '@/composables/useAsyncAction'
import NotificationItem from '@/components/NotificationItem.vue'

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
</style>
