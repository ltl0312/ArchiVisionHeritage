import request from './request'

export const zhixiApi = {
  analyze: (formData) => request.post('/v1/analysis/zhixi', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
