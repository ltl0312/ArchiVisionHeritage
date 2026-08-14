import { defineStore } from 'pinia'
import { ref } from 'vue'
import { notificationApi } from '@/api/notification'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  const notifications = ref([])
  const showBrocade = ref(false)

  async function fetchUnreadCount() {
    try {
      const res = await notificationApi.getUnreadCount()
      unreadCount.value = res.data.count
    } catch { /* 未登录时忽略 */ }
  }

  async function fetchNotifications() {
    try {
      const res = await notificationApi.list()
      notifications.value = res.data
    } catch { /* ignore */ }
  }

  async function markAsRead(id) {
    await notificationApi.markAsRead(id)
  }

  async function markAllRead() {
    await notificationApi.markAllAsRead()
  }

  function closeBrocade() {
    showBrocade.value = false
  }

  return {
    unreadCount, notifications, showBrocade,
    fetchUnreadCount, fetchNotifications, markAsRead, markAllRead, closeBrocade
  }
})
