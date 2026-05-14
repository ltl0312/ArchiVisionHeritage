import request from './request'

export const authApi = {
  login: (data) => request.post('/v1/auth/login', data),
  register: (data) => request.post('/v1/auth/register', data),
}
