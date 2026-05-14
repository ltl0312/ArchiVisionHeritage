import request from './request'

export const notificationApi = {
  list: () => request.get('/v1/notifications'),
  getUnreadCount: () => request.get('/v1/notifications/unread-count'),
  markAsRead: (id) => request.put(`/v1/notifications/${id}/read`),
  markAllAsRead: () => request.put('/v1/notifications/read-all'),
}
