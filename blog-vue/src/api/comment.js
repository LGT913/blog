import request from './request'

/**
 * 评论 API
 */
const commentApi = {
  /**
   * 创建评论（需登录）
   * @param {number} articleId
   * @param {string} content
   * @param {number|null} [parentId]
   * @returns {Promise<import('../types').Comment>}
   */
  create: (articleId, content, parentId) => {
    let url = `/api/comment/create?articleId=${articleId}&content=${encodeURIComponent(content)}`
    if (parentId != null) url += `&parentId=${parentId}`
    return request.post(url)
  },

  /**
   * 获取文章评论列表
   * @param {number} articleId
   * @returns {Promise<import('../types').Comment[]>}
   */
  getByArticle: (articleId) => request.get(`/api/comment/article/${articleId}`),

  /**
   * 删除评论（ADMIN）
   * @param {number} id
   * @returns {Promise<string>}
   */
  delete: (id) => request.delete(`/api/comment/delete/${id}`),

  /**
   * 删除文章所有评论（ADMIN）
   * @param {number} articleId
   * @returns {Promise<string>}
   */
  deleteByArticle: (articleId) => request.delete(`/api/comment/article/${articleId}`)
}

export default commentApi