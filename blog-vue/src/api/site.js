import request from './request'
import { SiteConfigKey } from '../utils/constants'

/**
 * 站点配置 API
 */
const siteApi = {
  /**
   * 获取站点配置
   * @param {string} configKey
   * @returns {Promise<import('../types').SiteConfig>}
   */
  getConfig: (configKey) =>
    request.get('/api/site/config', { params: { configKey } }),

  /**
   * 创建/更新站点配置（ADMIN）
   * @param {{ configKey: string, configValue: string, description: string }} data
   * @returns {Promise<import('../types').SiteConfig>}
   */
  createConfig: (data) => request.post('/api/site/config', data),

  /**
   * 删除站点配置（ADMIN）
   * @param {number} id
   * @returns {Promise<string>}
   */
  deleteConfig: (id) => request.delete(`/api/site/config/${id}`)
}

export default siteApi