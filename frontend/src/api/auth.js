import request from './request'

export const authApi = {
  login: (data) => request.post('/v1/auth/login', data),
  register: (data) => request.post('/v1/auth/register', data),
  getCurrentUser: () => request.get('/v1/users/me'),
  updateProfile: (data) => request.put('/v1/users/me', data),
  changePassword: (data) => request.put('/v1/users/me/password', data),
  getMyPosts: (page = 1, size = 12) => request.get('/v1/users/me/posts', { params: { page, size } }),
  getMyLikes: (page = 1, size = 12) => request.get('/v1/users/me/likes', { params: { page, size } })
}
