# API 接口文档

## 基础信息

- **基础地址**: `http://localhost:8080/api`
- **返回格式**: JSON
- **成功响应**: `{ "code": 200, "message": "成功", "data": {...} }`
- **失败响应**: `{ "code": 500, "message": "错误信息", "data": null }`

---

## 用户模块

### 1. 用户注册

**POST** `/user/register`

请求体:
```json
{
  "username": "string (用户名)",
  "password": "string (密码)",
  "nickname": "string (昵称)"
}
```

成功响应:
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "username": "test",
    "nickname": "测试用户"
  }
}
```

### 2. 用户登录

**POST** `/user/login`

请求体:
```json
{
  "username": "string (用户名)",
  "password": "string (密码)"
}
```

成功响应:
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "username": "test",
    "nickname": "测试用户"
  }
}
```

---

## 文章模块

### 1. 创建文章

**POST** `/article/create`

请求体:
```json
{
  "title": "string (标题，必填)",
  "content": "string (内容，必填)",
  "userId": "number (用户ID，必填)",
  "categoryId": "string (分类ID，必填)"
}
```

成功响应:
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "title": "文章标题",
    "content": "文章内容",
    "userId": 1,
    "categoryId": "1",
    "createTime": "2024-01-01T12:00:00",
    "updateTime": "2024-01-01T12:00:00"
  }
}
```

### 2. 获取文章详情

**GET** `/article/{id}`

路径参数:
| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 文章ID |

成功响应:
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "title": "文章标题",
    "content": "文章内容",
    "userId": 1,
    "categoryId": "1",
    "createTime": "2024-01-01T12:00:00",
    "updateTime": "2024-01-01T12:00:00"
  }
}
```

### 3. 获取文章列表

**GET** `/article/list`

成功响应:
```json
{
  "code": 200,
  "message": "成功",
  "data": [
    {
      "id": 1,
      "title": "文章标题",
      "content": "文章内容",
      "userId": 1,
      "categoryId": "1",
      "createTime": "2024-01-01T12:00:00",
      "updateTime": "2024-01-01T12:00:00"
    }
  ]
}
```

### 4. 获取用户文章

**GET** `/article/user/{userId}`

路径参数:
| 参数 | 类型 | 说明 |
|------|------|------|
| userId | Long | 用户ID |

成功响应: 同获取文章列表

### 5. 更新文章

**PUT** `/article/update/{id}?title={title}&content={content}`

路径参数:
| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 文章ID |

查询参数:
| 参数 | 类型 | 说明 |
|------|------|------|
| title | String | 新标题 |
| content | String | 新内容 |

成功响应:
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "title": "更新后的标题",
    "content": "更新后的内容",
    "userId": 1,
    "categoryId": "1",
    "createTime": "2024-01-01T12:00:00",
    "updateTime": "2024-01-01T12:00:00"
  }
}
```

### 6. 删除文章

**DELETE** `/article/delete/{id}`

路径参数:
| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 文章ID |

成功响应:
```json
{
  "code": 200,
  "message": "成功",
  "data": "删除成功"
}
```

---

## 分类模块

### 1. 创建分类

**POST** `/category/create`

请求体:
```json
{
  "name": "string (分类名称，必填)",
  "description": "string (分类描述)"
}
```

成功响应:
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "name": "技术",
    "description": "技术相关文章"
  }
}
```

### 2. 获取分类详情

**GET** `/category/{id}`

路径参数:
| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 分类ID |

成功响应:
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "name": "技术",
    "description": "技术相关文章"
  }
}
```

### 3. 获取分类列表

**GET** `/category/list`

成功响应:
```json
{
  "code": 200,
  "message": "成功",
  "data": [
    {
      "id": 1,
      "name": "技术",
      "description": "技术相关文章"
    }
  ]
}
```

### 4. 更新分类

**PUT** `/category/update/{id}`

请求体:
```json
{
  "name": "string (新分类名称)",
  "description": "string (新分类描述)"
}
```

成功响应:
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "name": "更新后的分类",
    "description": "更新后的描述"
  }
}
```

### 5. 删除分类

**DELETE** `/category/delete/{id}`

路径参数:
| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 分类ID |

成功响应:
```json
{
  "code": 200,
  "message": "成功",
  "data": "删除成功"
}
```

---

## 评论模块

### 1. 创建评论

**POST** `/comment/create?articleId={articleId}&userId={userId}&content={content}&parentId={parentId}`

查询参数:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| articleId | Long | 是 | 文章ID |
| userId | Long | 是 | 用户ID |
| content | String | 是 | 评论内容 |
| parentId | Long | 否 | 父评论ID（回复时使用） |

成功响应:
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "id": 1,
    "articleId": 1,
    "userId": 1,
    "content": "评论内容",
    "parentId": null,
    "createTime": "2024-01-01T12:00:00"
  }
}
```

### 2. 获取文章评论

**GET** `/comment/article/{articleId}`

路径参数:
| 参数 | 类型 | 说明 |
|------|------|------|
| articleId | Long | 文章ID |

成功响应:
```json
{
  "code": 200,
  "message": "成功",
  "data": [
    {
      "id": 1,
      "articleId": 1,
      "userId": 1,
      "content": "评论内容",
      "parentId": null,
      "createTime": "2024-01-01T12:00:00"
    }
  ]
}
```

### 3. 删除评论

**DELETE** `/comment/delete/{id}`

路径参数:
| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 评论ID |

成功响应:
```json
{
  "code": 200,
  "message": "成功",
  "data": "删除成功"
}
```

### 4. 删除文章所有评论

**DELETE** `/comment/article/{articleId}`

路径参数:
| 参数 | 类型 | 说明 |
|------|------|------|
| articleId | Long | 文章ID |

成功响应:
```json
{
  "code": 200,
  "message": "成功",
  "data": "删除成功"
}
```

---

## 数据模型

### User (用户)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 用户ID |
| username | String | 用户名 |
| password | String | 密码 |
| nickname | String | 昵称 |

### Article (文章)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 文章ID |
| title | String | 标题 |
| content | String | 内容（Lob类型） |
| userId | Long | 用户ID |
| categoryId | String | 分类ID |
| createTime | LocalDateTime | 创建时间 |
| updateTime | LocalDateTime | 更新时间 |

### Category (分类)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 分类ID |
| name | String | 分类名称 |
| description | String | 分类描述 |

### Comment (评论)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 评论ID |
| articleId | Long | 文章ID |
| userId | Long | 用户ID |
| content | String | 评论内容 |
| parentId | Long | 父评论ID |
| createTime | LocalDateTime | 创建时间 |
