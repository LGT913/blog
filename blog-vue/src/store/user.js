import { reactive } from 'vue'

const state = reactive({
  user: null,
  token: null,
  isLoggedIn: false
})

// 退出登录时的回调函数列表（用于通知其他模块）
let logoutCallbacks = []

/**
 * 注册退出登录回调
 * @param {Function} callback
 */
const onLogout = (callback) => {
  logoutCallbacks.push(callback)
}

const setUser = (user, token) => {
  state.user = user
  state.token = token
  state.isLoggedIn = !!user
  if (user) {
    localStorage.setItem('blog_user', JSON.stringify(user))
    if (token) localStorage.setItem('blog_token', token)
  } else {
    localStorage.removeItem('blog_user')
    localStorage.removeItem('blog_token')
  }
}

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
  localStorage.removeItem('blog_user')
  localStorage.removeItem('blog_token')

  // 通知所有监听者：用户已退出
  logoutCallbacks.forEach(cb => {
    try { cb() } catch (e) { console.error('退出回调执行失败:', e) }
  })
}

const clearAuth = () => {
  state.user = null
  state.token = null
  state.isLoggedIn = false
  localStorage.removeItem('blog_user')
  localStorage.removeItem('blog_token')
}

const init = () => {
  const savedUser = localStorage.getItem('blog_user')
  const savedToken = localStorage.getItem('blog_token')
  if (savedUser && savedToken) {
    try {
      setUser(JSON.parse(savedUser), savedToken)
    } catch (e) {
      localStorage.removeItem('blog_user')
      localStorage.removeItem('blog_token')
    }
  }
}

export function useUserStore() {
  return {
    state,
    login,
    logout,
    onLogout,
    clearAuth,
    init
  }
}