import axios from 'axios'
import { ResponseCode, StorageKey, RequestTimeout } from '../utils/constants'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: RequestTimeout,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器：自动附加 Authorization
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(StorageKey.TOKEN)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一解包 + 错误处理
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === ResponseCode.SUCCESS) {
      return res.data
    }

    // 401 未授权：token 过期 / 被踢下线
    if (res.code === ResponseCode.UNAUTHORIZED) {
      // skipAuthRedirect: true 的请求（如点赞）401 是预期行为，不触发全局退出
      if (response.config?.skipAuthRedirect) {
        return Promise.reject(new Error(res.message || '登录已过期'))
      }
      localStorage.removeItem(StorageKey.TOKEN)
      localStorage.removeItem(StorageKey.USER)
      window.dispatchEvent(new CustomEvent('auth:expired'))
      return Promise.reject(new Error(res.message || '登录已过期'))
    }

    // 403 权限不足
    if (res.code === ResponseCode.FORBIDDEN) {
      window.dispatchEvent(new CustomEvent('auth:forbidden'))
      return Promise.reject(new Error(res.message || '权限不足'))
    }

    // 其他错误（500 等）
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    // 超时
    if (error.message && error.message.includes('timeout')) {
      return Promise.reject(new Error('请求超时，请稍后重试'))
    }
    // 网络断开 / 后端宕机
    if (error.message && error.message.includes('Network Error')) {
      return Promise.reject(new Error('无法连接到服务器，请检查后端服务是否启动'))
    }

    // HTTP 状态码错误（后端可能直接返回 401/403 而非业务 JSON）
    const status = error.response?.status
    const serverMsg = error.response?.data?.message || error.response?.data?.msg

    if (status === 401) {
      // skipAuthRedirect: true 的请求（如点赞）401 是预期行为，不触发全局退出
      if (error.config?.skipAuthRedirect) {
        return Promise.reject(new Error(serverMsg || '登录已过期，请重新登录'))
      }
      localStorage.removeItem(StorageKey.TOKEN)
      localStorage.removeItem(StorageKey.USER)
      window.dispatchEvent(new CustomEvent('auth:expired'))
      return Promise.reject(new Error(serverMsg || '登录已过期，请重新登录'))
    }

    if (status === 403) {
      window.dispatchEvent(new CustomEvent('auth:forbidden'))
      return Promise.reject(new Error(serverMsg || '权限不足，无法执行此操作'))
    }

    if (status === 404) {
      return Promise.reject(new Error(serverMsg || '请求的接口不存在'))
    }

    if (status && status >= 500) {
      return Promise.reject(new Error(serverMsg || '服务器内部错误，请稍后重试'))
    }

    // 其他未知错误，保留原始错误信息
    return Promise.reject(error)
  }
)

export default request