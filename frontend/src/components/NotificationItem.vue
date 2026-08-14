<template>
  <div
    class="notification-item"
    :class="{ unread: !notification.read }"
    @click="$emit('click', notification)"
  >
    <div class="notif-icon">
      <el-icon size="20" :color="notification.read ? 'var(--color-text-muted)' : 'var(--color-accent)'">
        <Present />
      </el-icon>
    </div>
    <div class="notif-body">
      <p class="notif-message">{{ notification.message }}</p>
      <span class="notif-time">{{ notification.createdAt }}</span>
    </div>
    <div v-if="!notification.read" class="notif-dot"></div>
  </div>
</template>

<script setup>
import { Present } from '@element-plus/icons-vue'

defineProps({
  notification: { type: Object, required: true }
})

defineEmits(['click'])
</script>

<style scoped>
/* ═══ 通知条目（原 NotificationListView scoped 通知样式逐字搬移）═══ */
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
