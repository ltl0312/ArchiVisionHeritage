import request from './request'

export const communityApi = {
  getPosts: (page = 1, size = 12) => request.get('/v1/posts', { params: { page, size } }),
  getPostDetail: (id) => request.get(`/v1/posts/${id}`),
  createPost: (data) => request.post('/v1/posts', data),
  addComment: (postId, data) => request.post(`/v1/posts/${postId}/comments`, data),
  toggleLike: (data) => request.post('/v1/interactions/like', data),
  toggleFollow: (userId) => request.post(`/v1/users/${userId}/follow`),
}
