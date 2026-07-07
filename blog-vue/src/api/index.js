const BASE_URL = '/api'

const request = async (url, options = {}) => {
  try {
    console.log(`[API请求] ${url}`, options)
    
    const response = await fetch(`${BASE_URL}${url}`, {
      headers: {
        'Content-Type': 'application/json',
        ...options.headers
      },
      ...options
    })
    
    console.log(`[API响应] ${url} status: ${response.status}`)
    
    if (!response.ok) {
      const errorText = await response.text()
      console.error(`[API错误] ${url} body:`, errorText)
      throw new Error(`请求失败: ${response.status} ${response.statusText}`)
    }
    
    const text = await response.text()
    console.log(`[API数据] ${url} body:`, text)
    
    let data
    try {
      data = JSON.parse(text)
    } catch (e) {
      throw new Error('服务器返回数据格式错误')
    }
    
    if (data.code !== 200) {
      throw new Error(data.message || '请求失败')
    }
    return data.data
  } catch (e) {
    console.error(`[API异常] ${url}:`, e.message)
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
  list: () => request('/article/list'),
  getUserArticles: (userId) => request(`/article/user/${userId}`),
  update: (id, title, content) => request(`/article/update/${id}?title=${encodeURIComponent(title)}&content=${encodeURIComponent(content)}`, {
    method: 'PUT'
  }),
  delete: (id) => request(`/article/delete/${id}`, {
    method: 'DELETE'
  })
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
