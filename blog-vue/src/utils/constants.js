/**
 * 全局常量定义
 * 所有后端状态码、魔法数字集中管理，禁止在页面中硬编码
 */

// ========== 后端统一返回状态码 ==========
export const ResponseCode = {
  /** 成功 */
  SUCCESS: 200,
  /** 未授权 / token 过期 / 被踢下线 */
  UNAUTHORIZED: 401,
  /** 权限不足 */
  FORBIDDEN: 403,
  /** 服务端异常 */
  ERROR: 500
}

// ========== 用户角色 ==========
export const UserRole = {
  USER: 'USER',
  ADMIN: 'ADMIN'
}

// ========== 点赞动作 ==========
export const LikeAction = {
  LIKE: 'like',
  UNLIKE: 'unlike'
}

// ========== 分页默认值 ==========
export const Pagination = {
  /** 默认页码（后端从 0 开始） */
  DEFAULT_PAGE: 0,
  /** 默认每页条数 */
  DEFAULT_SIZE: 10,
  /** 最大每页条数 */
  MAX_SIZE: 100
}

// ========== 站点配置 Key ==========
export const SiteConfigKey = {
  BLOG_INFO: 'blog_info'
}

// ========== 本地存储 Key ==========
export const StorageKey = {
  TOKEN: 'blog_token',
  USER: 'blog_user',
  SITE_CONFIG: 'blog_site_config'
}

// ========== 缓存有效期（毫秒） ==========
export const CacheDuration = {
  SITE_CONFIG: 24 * 60 * 60 * 1000 // 24 小时
}

// ========== 请求超时时间（毫秒） ==========
export const RequestTimeout = 15000