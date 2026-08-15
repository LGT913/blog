import request from './request'

/**
 * 文章 API
 */
const articleApi = {
  /**
   * 创建文章（需登录）
   * @param {{ title: string, content: string, categoryId: string }} data
   * @returns {Promise<import('../types').Article>}
   */
  create: (data) => request.post('/api/article/create', data),

  /**
   * 获取文章详情
   * @param {number} id
   * @returns {Promise<import('../types').Article>}
   */
  get: (id) => request.get(`/api/article/${id}`),

  /**
   * 文章列表（分页，page 从 0 开始）
   * @param {number} [page=0]
   * @param {number} [size=10]
   * @param {string} [categoryId]
   * @param {string} [keyword]
   * @returns {Promise<import('../types').PageResult<import('../types').Article>>}
   */
  list: (page = 0, size = 10, categoryId = '', keyword = '') => {
    const params = { page, size }
    if (categoryId) params.categoryId = categoryId
    if (keyword) params.keyword = keyword
    return request.get('/api/article/list', { params })
  },

  /**
   * 获取某用户的文章列表
   * @param {number} userId
   * @returns {Promise<import('../types').Article[]>}
   */
  getUserArticles: (userId) => request.get(`/api/article/user/${userId}`),

  /**
   * 更新文章（ADMIN）
   * @param {number} id
   * @param {{ title: string, content: string, categoryId: string }} data
   * @returns {Promise<import('../types').Article>}
   */
  update: (id, data) => request.put(`/api/article/update/${id}`, data),

  /**
   * 删除文章（ADMIN）
   * @param {number} id
   * @returns {Promise<string>}
   */
  delete: (id) => request.delete(`/api/article/delete/${id}`),

  /**
   * 浏览量排行
   * @returns {Promise<import('../types').Article[]>}
   */
  rankingByViews: () => request.get('/api/article/ranking/views'),

  /**
   * 最新文章排行
   * @returns {Promise<import('../types').Article[]>}
   */
  rankingByLatest: () => request.get('/api/article/ranking/latest'),

  /**
   * 文章搜索（分页，page 从 0 开始）
   * @param {string} keyword
   * @param {number} [page=0]
   * @param {number} [size=10]
   * @returns {Promise<import('../types').PageResult<import('../types').Article>>}
   */
  search: (keyword, page = 0, size = 10) =>
    request.get('/api/article/search', { params: { keyword, page, size } }),

  /**
   * 定时发布文章（需登录）
   * @param {{ title: string, content: string, categoryId: string }} data
   * @returns {Promise<string>}
   */
  schedulePublish: (data) => request.post('/api/article/schedulePublish', data)
}

export default articleApi