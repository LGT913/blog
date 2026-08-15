import request from './request'

/**
 * 通知 API
 */
const noticeApi = {
  /**
   * 通知列表
   * @returns {Promise<import('../types').Notice[]>}
   */
  list: () => request.get('/api/notice/list'),

  /**
   * 创建通知（ADMIN）
   * @param {{ title: string, content: string }} data
   * @returns {Promise<import('../types').Notice>}
   */
  create: (data) => request.post('/api/notice/create', data),

  /**
   * 更新通知（ADMIN）
   * @param {number} id
   * @param {{ title: string, content: string }} data
   * @returns {Promise<import('../types').Notice>}
   */
  update: (id, data) => request.put(`/api/notice/update/${id}`, data),

  /**
   * 删除通知（ADMIN）
   * @param {number} id
   * @returns {Promise<string>}
   */
  delete: (id) => request.delete(`/api/notice/delete/${id}`)
}

export default noticeApi