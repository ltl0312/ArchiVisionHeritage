import request from './request'

export const adminApi = {
  getPendingPosts: (page = 1, size = 20) => request.get('/v1/admin/posts/pending', { params: { page, size } }),
  auditPost: (id, status, rejectReason) => request.put(`/v1/admin/posts/${id}/audit`, { status, rejectReason: rejectReason || null })
}
