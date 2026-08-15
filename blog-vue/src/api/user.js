import request from './request'

/**
 * 用户 API
 */
const userApi = {
  /**
   * 用户注册
   * @param {{ username: string, password: string, nickname: string }} data
   * @returns {Promise<import('../types').User>}
   */
  register: (data) => request.post('/api/user/register', data),

  /**
   * 用户登录
   * @param {{ username: string, password: string }} data
   * @returns {Promise<{ user: import('../types').User, token: string }>}
   */
  login: (data) => request.post('/api/user/login', data),

  /**
   * 用户退出
   * @returns {Promise<string>}
   */
  logout: () => request.post('/api/user/logout'),

  /**
   * 管理员踢出用户
   * @param {number} userId
   * @returns {Promise<string>}
   */
  kick: (userId) => request.post(`/api/user/admin/kick/${userId}`),

  /**
   * 管理员启用/禁用用户
   * @param {number} userId
   * @returns {Promise<string>}
   */
  enable: (userId) => request.post(`/api/user/admin/enable/${userId}`)
}

export default userApi