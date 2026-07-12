import { reactive } from 'vue'
import { siteApi } from '../api'

// 默认兜底配置（接口失败或数据为空时使用）
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

const loadConfig = async () => {
  state.loading = true
  try {
    const data = await siteApi.getConfig('blog_info')
    state.config = parseSiteConfig(data)
  } catch (e) {
    console.error('获取站点配置失败，使用默认配置:', e)
    state.config = { ...defaultConfig }
  } finally {
    state.loading = false
  }
}

export function useSiteStore() {
  return {
    state,
    loadConfig
  }
}
