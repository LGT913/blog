# 个人博客系统

基于 **Spring Boot + Vue 3** 的现代化个人博客系统，包含**单体版**（已上线）与**微服务版**（演进实验）两套后端实现，支持用户、文章、分类、评论、点赞、通知、AI 摘要等功能。

## ✨ 功能特性

- **用户模块**：注册、登录、退出、个人中心、JWT 认证 + 角色权限（USER / ADMIN）
- **文章模块**：创建、编辑、删除、查看详情、分页/分类/关键词搜索、排行榜
- **分类模块**：分类创建、编辑、删除、列表展示
- **评论模块**：评论发布、删除、按文章查询、WebSocket 实时推送
- **互动模块**：点赞、浏览历史记录
- **通知模块**：站内通知（评论通知等，RabbitMQ 异步投递）
- **AI 摘要**：接入 DeepSeek API 异步生成文章摘要（MQ 消费 + 失败重试）
- **网站配置**：管理员在线配置站点信息（`/api/site/config`）
- **性能优化**：Redis 缓存（列表/详情/排行）、布隆过滤器防缓存穿透、Redisson 分布式锁、Redis 令牌桶限流

## 🛠️ 技术栈

### 单体版 `blog/`（Spring Boot 4.x，Java 17，端口 8088）

| 技术 | 说明 |
|------|------|
| Spring Boot 4.0.6 + WebMVC | 后端框架 |
| Spring Data JPA | ORM |
| Spring Security + JWT | 认证授权（FilterChain） |
| MySQL 8 + Redis 7 + RabbitMQ | 数据 / 缓存 / 异步消息 |
| Redisson | 分布式锁 + 布隆过滤器 |
| DeepSeek API | AI 摘要生成 |

### 微服务版 `blog-cloud/`（Spring Boot 3.5.4 + Spring Cloud Alibaba）

| 模块 | 说明 |
|------|------|
| `blog-gateway` | 网关（Spring Cloud Gateway，WebFlux） |
| `blog-user-service` | 用户服务 |
| `blog-article-service` | 文章 / 分类 / 评论服务（Feign 调用用户服务） |
| `blog-common` | 公共模块（统一响应、异常、常量） |
| Nacos / Feign / Sentinel | 注册中心 / 远程调用 / 限流熔断 |

### 前端 `blog-vue/`（Vue 3）

| 技术 | 版本 |
|------|------|
| Vue | 3.5.x |
| Vue Router | 4.x |
| Vite | 8.x |
| Tailwind CSS | 3.4 |
| Axios | 1.x |
| SockJS + STOMP | WebSocket 评论推送 |

## 📁 项目结构

```
blog/
├── blog/                    # 单体后端（已上线部署）
│   └── src/main/java/com/blog/blog/
│       ├── controller/      # REST API 控制器
│       ├── service/         # 业务逻辑 + MQ 消费者
│       ├── repository/      # 数据访问层
│       ├── entity/          # 实体类
│       ├── dto/ vo/         # 传输对象 / 视图对象
│       ├── config/          # 安全、缓存、MQ、WebSocket 等配置
│       └── common/          # 工具类、统一响应、异常处理
├── blog-cloud/              # 微服务版（Nacos / Feign / Gateway）
│   ├── blog-gateway/        # 网关
│   ├── blog-user-service/   # 用户服务
│   ├── blog-article-service # 文章服务
│   ├── blog-common/         # 公共模块
│   └── docker-compose.yml   # 微服务中间件编排
├── blog-vue/                # 前端项目
│   ├── src/views/           # 页面
│   ├── src/components/      # 组件
│   ├── src/api/             # API 请求封装
│   ├── src/router/          # 路由配置
│   ├── src/store/           # 状态管理（Vue 响应式）
├── docker-compose.yml       # 本地开发中间件（Redis/RabbitMQ/Kafka）
├── docker/nginx/            # Nginx 配置
├── API.md                   # API 接口文档
└── backend-architecture-doc.md  # 后端架构详解
```

## 🚀 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+ / Redis 7.0+ / RabbitMQ（本地开发可用根目录 `docker-compose.yml` 拉起，MySQL 需自行安装）
- Maven 3.8+

### 1. 启动中间件（Docker Compose）

根目录 `docker-compose.yml` 提供本地开发中间件：Redis(6379)、RabbitMQ(5672, 管理台 15672)、Kafka(9092)。MySQL 需自行安装，或在服务器端使用 `docker-compose.yml`（生产版）编排 MySQL/Redis/RabbitMQ。

```bash
docker compose up -d
```

### 2. 启动单体后端

配置通过**环境变量**注入（生产必填），本地开发可留默认值，详见 [application.yml](blog/src/main/resources/application.yml)：

| 环境变量 | 默认值 | 说明 |
|----------|--------|------|
| `DB_HOST` / `DB_PORT` | `localhost` / `3306` | 数据库地址 |
| `DB_USERNAME` | `root` | 数据库用户 |
| `DB_PASSWORD` | —（必填） | 数据库密码 |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis 地址 |
| `REDIS_PASSWORD` | 空 | Redis 密码 |
| `RABBITMQ_HOST` / `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | `127.0.0.1` / `admin` / — | 消息队列 |
| `JWT_SECRET_KEY` | 开发默认（生产必须覆盖） | JWT 签名密钥，用 `openssl rand -base64 48` 生成 |
| `DEEPSEEK_API_KEY` | 空 | AI 摘要密钥 |
| `CORS_ALLOWED_ORIGINS` | 本地前端 | 允许跨域来源，逗号分隔 |

```bash
cd blog
mvn spring-boot:run
# 或打包运行
mvn clean package -DskipTests
java -jar target/blog-0.0.1-SNAPSHOT.jar
```

服务启动后访问：`http://localhost:8088`

### 3. 启动前端

```bash
cd blog-vue
npm install
npm run dev
```

前端开发服务器：`http://localhost:5173`（`/api` 由 Vite 代理到 8088）

### 4. 创建管理员

默认注册用户角色为 `USER`，需在数据库升级为 ADMIN（网站配置入口在 `/admin` 管理面板）：

```sql
UPDATE `user` SET role='ADMIN' WHERE id=1;  -- 按实际 id 修改
```

> ⚠️ 修改角色后需**重新登录**，JWT 中的角色在登录时写入。

## 📦 生产部署

已上线于阿里云 ECS（详见 [单体部署上线.md](单体部署上线.md)）：

- 中间件：Docker Compose 编排（仅绑定 `127.0.0.1`，不对外暴露）
- 应用：jar + systemd 常驻（`/etc/systemd/system/blog.service`）
- 前端：构建为 dist，由 Nginx 托管静态资源并反代 `/api`、`/ws`

## 📖 文档

- [API.md](API.md) — 接口文档
- [backend-architecture-doc.md](backend-architecture-doc.md) — 后端架构与安全设计详解
- [单体部署上线.md](单体部署上线.md) — 服务器部署笔记

## 📄 许可证

MIT License
