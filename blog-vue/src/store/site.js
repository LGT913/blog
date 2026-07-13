import { reactive } from 'vue'
import { siteApi } from '../api'

// localStorage 缓存 key
const SITE_CONFIG_KEY = 'blog_site_config'
// 缓存有效期：24 小时（单位：毫秒）
const CACHE_MAX_AGE = 24 * 60 * 60 * 1000

// 默认兜底配置（接口失败、无缓存、退出登录时使用）
const defaultConfig = {
  siteName: 'My Blog',
  siteLogo: '',
  siteDesc: '分享 · 记录 · 成长',
  slogan: '记录思考与灵感的个人空间',
  copyright: 'All Rights Reserved',
  recordNo: '',
  email: ''
}

const state = reactive({
  loading: true,
  config: { ...defaultConfig }
})

// 标记是否已重置（防止 silentRefreshConfig 在重置后写入缓存）
let isReset = false

/**
 * 从 localStorage 读取缓存的配置
 * @returns {Object|null} 缓存有效返回配置对象，否则返回 null
 */
const getCachedConfig = () => {
  try {
    const cached = localStorage.getItem(SITE_CONFIG_KEY)
    if (!cached) return null

    const { data, timestamp } = JSON.parse(cached)
    // 检查缓存是否过期
    if (Date.now() - timestamp > CACHE_MAX_AGE) {
      localStorage.removeItem(SITE_CONFIG_KEY)
      return null
    }
    return data
  } catch (e) {
    console.error('读取配置缓存失败:', e)
    localStorage.removeItem(SITE_CONFIG_KEY)
    return null
  }
}

/**
 * 将配置写入 localStorage 缓存
 * @param {Object} configData 配置数据
 */
const setCachedConfig = (configData) => {
  // 如果已重置（退出登录状态），不再写入缓存
  if (isReset) {
    console.log('[siteStore] 已重置，跳过缓存写入')
    return
  }
  try {
    localStorage.setItem(SITE_CONFIG_KEY, JSON.stringify({
      data: configData,
      timestamp: Date.now()
    }))
  } catch (e) {
    console.error('写入配置缓存失败:', e)
  }
}

/**
 * 清除 localStorage 中的配置缓存
 * 退出登录时必须调用，防止脏数据残留
 */
const clearCachedConfig = () => {
  localStorage.removeItem(SITE_CONFIG_KEY)
  console.log('[siteStore] localStorage 配置缓存已清除')
}

// 解析后端返回的 JSON 字符串（config_value 字段）
const parseSiteConfig = (data) => {
  if (!data || !data.configValue) {
    return { ...defaultConfig }
  }
  try {
    const parsed = JSON.parse(data.configValue)
    return { ...defaultConfig, ...parsed }
  } catch (e) {
    console.error('站点配置 JSON 解析失败:', e)
    return { ...defaultConfig }
  }
}

/**
 * 加载站点配置
 * 核心逻辑：
 * 1. 用户未登录时，直接使用默认配置，不请求后端，不缓存
 * 2. 用户已登录时，优先读缓存展示，后台静默更新
 * 3. 退出登录后调用 resetConfig()，isReset=true 阻止任何缓存写入
 *
 * @param {Object} options
 * @param {boolean} options.forceRefresh 是否强制刷新，忽略缓存
 */
const loadConfig = async (options = {}) => {
  const { forceRefresh = false } = options

  // 检查用户登录状态：未登录时直接使用默认配置
  const token = localStorage.getItem('blog_token')
  if (!token) {
    console.log('[siteStore] 用户未登录，使用默认配置')
    isReset = true
    state.config = { ...defaultConfig }
    state.loading = false
    clearCachedConfig()
    return
  }

  // 用户已登录，允许正常加载和缓存
  isReset = false

  // 1. 非强制刷新时，先尝试从本地缓存读取（提升首屏速度）
  if (!forceRefresh) {
    const cached = getCachedConfig()
    if (cached) {
      state.config = cached
      state.loading = false
      // 后台静默更新，不阻塞页面渲染
      silentRefreshConfig()
      return
    }
  }

  // 2. 无缓存或强制刷新时，直接请求后端
  await fetchConfigFromServer()
}

/**
 * 从后端请求最新配置
 */
const fetchConfigFromServer = async () => {
  state.loading = true
  try {
    const data = await siteApi.getConfig('blog_info')
    const parsedConfig = parseSiteConfig(data)

    state.config = parsedConfig
    // 写入本地缓存，下次页面加载可直接使用
    setCachedConfig(parsedConfig)
  } catch (e) {
    console.error('获取站点配置失败，使用默认配置:', e)
    // 接口失败时，如果有旧缓存先用旧缓存，否则用默认配置
    const cached = getCachedConfig()
    state.config = cached || { ...defaultConfig }
  } finally {
    state.loading = false
  }
}

/**
 * 后台静默刷新配置（不修改 loading 状态）
 * 用于页面初始化时先展示缓存，再后台更新最新数据
 */
const silentRefreshConfig = async () => {
  try {
    const data = await siteApi.getConfig('blog_info')
    const parsedConfig = parseSiteConfig(data)

    // 只有当数据真正发生变化时才更新状态和缓存
    const currentConfigStr = JSON.stringify(state.config)
    const newConfigStr = JSON.stringify(parsedConfig)
    if (currentConfigStr !== newConfigStr) {
      // 检查是否已重置（退出登录），如果是则不再更新
      if (isReset) {
        console.log('[siteStore] 已重置，跳过后台更新')
        return
      }
      state.config = parsedConfig
      setCachedConfig(parsedConfig)
      console.log('[siteStore] 站点配置已后台更新')
    }
  } catch (e) {
    // 静默刷新失败不抛错，不影响用户体验
    console.warn('后台刷新配置失败:', e)
  }
}

/**
 * 重置为默认配置（退出登录时调用）
 * 同时清除 localStorage 缓存，设置 isReset 标志阻止后续缓存写入
 */
const resetConfig = () => {
  console.log('[siteStore] 重置配置为默认值')
  isReset = true
  state.config = { ...defaultConfig }
  clearCachedConfig()
}

export function useSiteStore() {
  return {
    state,
    loadConfig,
    resetConfig,
    // 导出供外部调试使用
    clearCachedConfig
  }
}
