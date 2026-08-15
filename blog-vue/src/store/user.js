import { reactive, computed } from 'vue'
import { StorageKey, UserRole } from '../utils/constants'

/**
 * 用户状态管理
 * 所有 localStorage 操作统一通过此 store 进行，不在组件中零散操作
 */
const state = reactive({
  user: null,
  token: null,
  isLoggedIn: false
})

/**
 * 计算属性：是否为管理员
 */
const isAdmin = computed(() => {
  return state.user && state.user.role === UserRole.ADMIN
})

/**
 * 计算属性：用户名快捷访问
 */
const username = computed(() => {
  return state.user ? state.user.username : ''
})

// 退出登录时的回调函数列表（用于通知其他模块，如 site store 清除配置缓存）
let logoutCallbacks = []

/**
 * 注册退出登录回调
 * @param {Function} callback
 */
const onLogout = (callback) => {
  logoutCallbacks.push(callback)
}

/**
 * 设置用户信息
 * @param {Object|null} user 用户对象
 * @param {string|null} token JWT token
 */
const setUser = (user, token) => {
  state.user = user
  state.token = token
  state.isLoggedIn = !!user
  if (user) {
    localStorage.setItem(StorageKey.USER, JSON.stringify(user))
    if (token) localStorage.setItem(StorageKey.TOKEN, token)
  } else {
    localStorage.removeItem(StorageKey.USER)
    localStorage.removeItem(StorageKey.TOKEN)
  }
}

/**
 * 登录
 * @param {Object} user 用户实体
 * @param {string} token JWT token
 */
const login = (user, token) => {
  setUser(user, token)
}

/**
 * 退出登录
 * 1. 清除用户状态和 token
 * 2. 触发所有注册的退出回调（如清除配置缓存）
 */
const logout = () => {
  state.user = null
  state.token = null
  state.isLoggedIn = false
  localStorage.removeItem(StorageKey.USER)
  localStorage.removeItem(StorageKey.TOKEN)

  // 通知所有监听者：用户已退出
  logoutCallbacks.forEach(cb => {
    try { cb() } catch (e) { console.error('退出回调执行失败:', e) }
  })
}

/**
 * 清除认证信息（token 过期时调用）
 * 不触发 logoutCallbacks，避免与 auth:expired 事件重复处理
 */
const clearAuth = () => {
  state.user = null
  state.token = null
  state.isLoggedIn = false
  localStorage.removeItem(StorageKey.USER)
  localStorage.removeItem(StorageKey.TOKEN)
}

/**
 * 判断 JWT 是否已过期（解析 payload 的 exp 字段）
 * @param {string} token
 * @returns {boolean} true 表示已过期
 */
const isTokenExpired = (token) => {
  try {
    const base64Url = token.split('.')[1]
    if (!base64Url) return false
    // base64url → base64（- → +，_ → /）
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const payload = JSON.parse(decodeURIComponent(escape(atob(base64))))
    return typeof payload.exp === 'number' && payload.exp * 1000 < Date.now()
  } catch (e) {
    // 解析失败不判定过期（交给后端校验），避免误杀
    return false
  }
}

/**
 * 初始化：从 localStorage 恢复登录状态
 * 包含 token 过期检测（JWT 解析 exp，过期则清除，避免假登录）
 */
const init = () => {
  const savedToken = localStorage.getItem(StorageKey.TOKEN)
  const savedUser = localStorage.getItem(StorageKey.USER)

  // token 不存在或为空，无需恢复
  if (!savedToken) {
    return
  }

  // token 已过期 → 清除登录态（防止过期 token 造成"假登录"，导致接口 401 被全局踢出）
  if (isTokenExpired(savedToken)) {
    clearAuth()
    return
  }

  // 尝试解析用户信息
  if (savedUser) {
    try {
      const user = JSON.parse(savedUser)
      // 基本校验：确保 necessary 字段存在
      if (user && user.id && user.username) {
        setUser(user, savedToken)
      } else {
        // 用户数据结构不完整，清除脏数据
        clearAuth()
      }
    } catch (e) {
      // JSON 解析失败，清除脏数据
      clearAuth()
    }
  } else {
    // 有 token 但没有 user 信息，数据不一致，清除
    clearAuth()
  }
}

export function useUserStore() {
  return {
    state,
    isAdmin,
    username,
    login,
    logout,
    onLogout,
    clearAuth,
    init
  }
}