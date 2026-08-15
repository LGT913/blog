import request from './request'

/**
 * 点赞 API
 */
const likeApi = {
  /**
   * 点赞/取消点赞（需登录）
   * @param {number} articleId
   * @param {'like'|'unlike'} action
   * @returns {Promise<{ liked: boolean, likeCount: number }>}
   */
  toggle: (articleId, action) =>
    request.post(`/api/like/article/${articleId}`, { action }, { skipAuthRedirect: true }),

  /**
   * 获取点赞状态（需登录）
   * @param {number} articleId
   * @returns {Promise<{ liked: boolean, likeCount: number }>}
   */
  status: (articleId) => request.get(`/api/like/status/${articleId}`, { skipAuthRedirect: true })
}

export default likeApi