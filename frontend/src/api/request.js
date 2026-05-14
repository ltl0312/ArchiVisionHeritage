import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',       // 配合 vite proxy，无需写完整地址
  timeout: 10000,
})

// 请求拦截器 — 可在此处添加 token
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) config.headers['Authorization'] = `Bearer ${token}`
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器 — 统一处理错误
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      // 业务异常：Result.code !== 200
      const msg = res.message || res.msg || '请求失败'
      ElMessage.error(msg)
      return Promise.reject(new Error(msg))
    }
    return res   // 直接返回 { code, message, data }
  },
  error => {
    // HTTP 异常（401/403/429/500等）— 优先提取后端返回的 message
    const serverMsg = error.response?.data?.message
    const msg = serverMsg || error.message || '网络错误'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default request
