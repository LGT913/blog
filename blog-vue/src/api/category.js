import request from './request'

/**
 * 分类 API
 */
const categoryApi = {
  /**
   * 分类列表
   * @returns {Promise<import('../types').Category[]>}
   */
  list: () => request.get('/api/category/list'),

  /**
   * 获取单个分类
   * @param {number} id
   * @returns {Promise<import('../types').Category>}
   */
  get: (id) => request.get(`/api/category/${id}`),

  /**
   * 创建分类（ADMIN）
   * @param {{ name: string, description: string }} data
   * @returns {Promise<import('../types').Category>}
   */
  create: (data) => request.post('/api/category/create', data),

  /**
   * 更新分类（ADMIN）
   * @param {number} id
   * @param {{ name: string, description: string }} data
   * @returns {Promise<import('../types').Category>}
   */
  update: (id, data) => request.put(`/api/category/update/${id}`, data),

  /**
   * 删除分类（ADMIN）
   * @param {number} id
   * @returns {Promise<string>}
   */
  delete: (id) => request.delete(`/api/category/delete/${id}`)
}

export default categoryApi