const BASE_URL = '/api'

let clearAuthCallback = null

export const setClearAuthCallback = (callback) => {
  clearAuthCallback = callback
}

const handleTokenExpired = () => {
  localStorage.removeItem('blog_token')
  localStorage.removeItem('blog_user')
  if (clearAuthCallback) {
    clearAuthCallback()
  }
  if (window.location.pathname !== '/') {
    window.location.href = '/'
  }
}

const request = async (url, options = {}) => {
  try {
    const token = localStorage.getItem('blog_token')
    const headers = {
      'Content-Type': 'application/json',
    }
    if (token) {
      headers['Authorization'] = token
    }

    const response = await fetch(`${BASE_URL}${url}`, {
      ...options,
      headers: {
        ...headers,
        ...(options.headers || {})
      }
    })

    if (response.status === 401) {
      handleTokenExpired()
      throw new Error('登录已过期，请重新登录')
    }

    if (!response.ok) {
      const errorText = await response.text()
      let errorMessage = `请求失败: ${response.status} ${response.statusText}`
      try {
        const errorData = JSON.parse(errorText)
        if (errorData.message) {
          errorMessage = errorData.message
        }
      } catch (e) {}
      throw new Error(errorMessage)
    }

    const text = await response.text()

    let data
    try {
      data = JSON.parse(text)
    } catch (e) {
      throw new Error('服务器返回数据格式错误')
    }

    if (data.code !== 200) {
      if (data.code === 401) {
        handleTokenExpired()
      }
      throw new Error(data.message || '请求失败')
    }
    return data.data
  } catch (e) {
    if (e.message.includes('Failed to fetch')) {
      throw new Error('无法连接到服务器，请检查后端服务是否启动')
    }
    throw e
  }
}

export const userApi = {
  register: (user) => request('/user/register', {
    method: 'POST',
    body: JSON.stringify(user)
  }),
  login: (user) => request('/user/login', {
    method: 'POST',
    body: JSON.stringify(user)
  })
}

export const articleApi = {
  create: (article) => request('/article/create', {
    method: 'POST',
    body: JSON.stringify(article)
  }),
  get: (id) => request(`/article/${id}`),
  list: (page = 0, size = 10) => {
    // 防御性编程：确保参数是数字
    const pageNum = Number(page)
    const sizeNum = Number(size)
    if (isNaN(pageNum) || isNaN(sizeNum)) {
      console.error('[API] articleApi.list 非法参数:', { page, size, pageNum, sizeNum })
      return Promise.reject(new Error('分页参数必须是数字'))
    }
    console.log('[API] articleApi.list 请求:', { page: pageNum, size: sizeNum })
    return request(`/article/list?page=${pageNum}&size=${sizeNum}`)
  },
  getUserArticles: (userId) => request(`/article/user/${userId}`),
  update: (id, title, content, categoryId) => request(`/article/update/${id}`, {
    method: 'PUT',
    body: JSON.stringify({ title, content, categoryId })
  }),
  delete: (id) => request(`/article/delete/${id}`, {
    method: 'DELETE'
  }),
  rankingByViews: () => request('/article/ranking/views'),
  rankingByLatest: () => request('/article/ranking/latest')
}

export const categoryApi = {
  create: (name, description) => request('/category/create', {
    method: 'POST',
    body: JSON.stringify({ name, description })
  }),
  get: (id) => request(`/category/${id}`),
  list: () => request('/category/list'),
  update: (id, name, description) => request(`/category/update/${id}`, {
    method: 'PUT',
    body: JSON.stringify({ name, description })
  }),
  delete: (id) => request(`/category/delete/${id}`, {
    method: 'DELETE'
  })
}

export const noticeApi = {
  list: () => request('/notice/list')
}

export const siteApi = {
  getConfig: (configKey) => request(`/site/config?configKey=${encodeURIComponent(configKey)}&_t=${Date.now()}`),
  updateConfig: (configKey, configValue) => request(`/site/config`, {
    method: 'POST',
    body: JSON.stringify({ configKey, configValue })
  })
}
