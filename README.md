# 个人博客系统

基于 Spring Boot + Vue3 构建的现代化个人博客系统，支持用户管理、文章发布、分类管理和评论功能。

## ✨ 功能特性

- **用户模块**：用户注册、登录、退出
- **文章模块**：文章创建、编辑、删除、查看详情
- **分类模块**：分类创建、编辑、删除、列表展示
- **评论模块**：评论发布、删除、按文章查询
- **搜索功能**：文章标题和内容搜索
- **分类筛选**：按分类筛选文章

## 🛠️ 技术栈

### 后端
| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.x | 后端框架 |
| Spring Data JPA | 3.x | ORM 框架 |
| MySQL | 8.x | 数据库 |
| Redis | 7.x | 缓存 |
| Maven | 3.x | 构建工具 |

### 前端
| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.x | 前端框架 |
| Vue Router | 4.x | 路由管理 |
| Vite | 8.x | 构建工具 |
| Tailwind CSS | 3.x | 样式框架 |
| Axios | 1.x | HTTP 请求 |

## 📁 项目结构

```
blog/
├── blog/                         # 后端项目
│   ├── src/
│   │   └── main/
│   │       ├── java/com/blog/blog/
│   │       │   ├── controller/      # REST API 控制器
│   │       │   ├── service/         # 业务逻辑层
│   │       │   ├── repository/      # 数据访问层
│   │       │   ├── entity/          # 实体类
│   │       │   ├── config/          # 配置类
│   │       │   └── common/          # 通用工具类
│   │       └── resources/
│   │           ├── application.properties  # 应用配置
│   │           └── 1.sql            # 数据库初始化脚本
│   └── pom.xml                      # Maven 配置
├── blog-vue/                       # 前端项目
│   ├── src/
│   │   ├── components/          # 组件
│   │   ├── views/               # 页面
│   │   ├── api/                 # API 请求封装
│   │   ├── router/              # 路由配置
│   │   └── store/               # 状态管理
│   └── vite.config.js           # Vite 配置
├── .gitignore                   # Git 忽略规则
├── README.md                    # 项目说明
└── API.md                       # API 文档
```

## 🚀 快速开始

### 环境要求

- JDK 21+
- Node.js 18+
- MySQL 8.0+
- Redis 7.0+

### 后端启动

1. **创建数据库**

```sql
CREATE DATABASE blog_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. **配置数据库连接**

修改 `blog/src/main/resources/application.properties`：

```properties
spring.datasource.username=root
spring.datasource.password=你的密码
```

3. **启动后端服务**

```bash
# 进入后端目录
cd blog

# 使用 Maven 运行
mvn spring-boot:run

# 或打包后运行
mvn clean package
java -jar target/blog-0.0.1-SNAPSHOT.jar
```

服务启动后访问：`http://localhost:8080`

### 前端启动

```bash
# 进入前端目录
cd blog-vue

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端服务启动后访问：`http://localhost:5173`

## 📖 API 文档

详细的 API 接口文档请查看 [API.md](API.md)

## 🔧 配置说明

### 后端配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| server.port | 8080 | 服务端口 |
| spring.datasource.url | jdbc:mysql://localhost:3306/blog_db | 数据库连接 |
| spring.data.redis.host | localhost | Redis 地址 |
| spring.data.redis.port | 6379 | Redis 端口 |

### 前端配置

前端代理配置在 `blog-vue/vite.config.js` 中，默认将 `/api` 请求转发到 `http://localhost:8080`

## 📄 许可证

MIT License
