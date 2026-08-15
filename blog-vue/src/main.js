import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'
import { useUserStore } from './store/user'
import { useSiteStore } from './store/site'
import { StorageKey } from './utils/constants'

// 提前初始化 stores，确保页面渲染时状态已就绪
const userStore = useUserStore()
const siteStore = useSiteStore()

// 1. 恢复登录状态（从 localStorage 读取）
userStore.init()

// 2. 加载站点配置（异步，不阻塞页面渲染）
siteStore.loadConfig()

// 3. 监听全局 auth:expired 事件（由 request.js 拦截器中 401 响应时触发）
window.addEventListener('auth:expired', () => {
  userStore.clearAuth()
  siteStore.resetConfig()
  // 触发登录弹窗（由 App.vue 监听 open:login 事件）
  window.dispatchEvent(new CustomEvent('open:login'))
})

// 4. 监听全局 auth:forbidden 事件（403 权限不足）
window.addEventListener('auth:forbidden', () => {
  console.warn('权限不足，当前用户无权执行此操作')
})

// 5. 全局未捕获 Promise 错误处理
window.addEventListener('unhandledrejection', (event) => {
  console.error('未捕获的 Promise 错误:', event.reason)
  // 401 错误由 api/index.js 的 request 拦截器统一处理
  // 这里仅记录日志，不阻止默认行为
})

const app = createApp(App)
app.use(router)
app.mount('#app')