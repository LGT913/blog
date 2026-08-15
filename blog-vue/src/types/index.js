/**
 * 统一响应格式
 * @typedef {Object} Result<T>
 * @property {number} code - 状态码 200成功 401未授权 403权限不足 500服务异常
 * @property {string} message - 响应消息
 * @property {T} data - 响应数据
 */

/**
 * @typedef {Object} User
 * @property {number} id
 * @property {string} username
 * @property {string} password
 * @property {string} nickname
 * @property {string} createTime
 * @property {'USER'|'ADMIN'} role
 * @property {boolean} enabled
 */

/**
 * @typedef {Object} Article
 * @property {number} id
 * @property {string} title
 * @property {string} content
 * @property {number} userId
 * @property {string} categoryId
 * @property {string} createTime
 * @property {string} updateTime
 * @property {string} summary
 * @property {number} viewCount
 * @property {number} likeCount
 */

/**
 * @typedef {Object} Comment
 * @property {number} id
 * @property {number} articleId
 * @property {number} userId
 * @property {string} content
 * @property {number|null} parentId
 * @property {string} createTime
 */

/**
 * @typedef {Object} Category
 * @property {number} id
 * @property {string} name
 * @property {string} description
 */

/**
 * @typedef {Object} Notice
 * @property {number} id
 * @property {string} title
 * @property {string} content
 * @property {string} createTime
 * @property {string} updateTime
 */

/**
 * @typedef {Object} SiteConfig
 * @property {number} id
 * @property {string} configKey
 * @property {string} configValue
 * @property {string} description
 * @property {string} createTime
 * @property {string} updateTime
 */

/**
 * @typedef {Object} PageResult<T>
 * @property {T[]} content
 * @property {number} totalElements
 * @property {number} totalPages
 * @property {number} number
 * @property {number} size
 * @property {boolean} first
 * @property {boolean} last
 * @property {boolean} hasNext
 * @property {boolean} hasPrevious
 */

export default {}