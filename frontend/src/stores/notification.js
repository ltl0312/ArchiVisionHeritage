import { defineStore } from 'pinia'
import { ref } from 'vue'
import { notificationApi } from '@/api/notification'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  const notifications = ref([])
  const showBrocade = ref(false)
  const brocadeTaskId = ref(null)

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

  function checkTaskSuccess(taskStatusRes) {
    if (taskStatusRes && taskStatusRes.status === 'SUCCESS') {
      brocadeTaskId.value = taskStatusRes.taskId
      showBrocade.value = true
      fetchUnreadCount()
    }
  }

  function closeBrocade() {
    showBrocade.value = false
  }

  return {
    unreadCount, notifications, showBrocade, brocadeTaskId,
    fetchUnreadCount, fetchNotifications, checkTaskSuccess, closeBrocade
  }
})
