# 个人博客后端架构文档

## 目录

1. [整体架构概览](#1-整体架构概览)
2. [第一步：基础 CRUD 搭建](#2-第一步基础-crud-搭建)
3. [第二步：统一响应与异常处理](#3-第二步统一响应与异常处理)
4. [第三步：Spring Security + JWT 认证](#4-第三步spring-security--jwt-认证)
5. [第四步：角色权限系统](#5-第四步角色权限系统)
6. [第五步：Redis 缓存优化](#6-第五步redis-缓存优化)
7. [第六步：布隆过滤器防缓存穿透](#7-第六步布隆过滤器防缓存穿透)
8. [技术栈与版本](#8-技术栈与版本)

---

## 1. 整体架构概览

项目采用经典的三层架构，分层职责如下：

```
┌─────────────────────────────────────────────────┐
│                   客户端 (Vue 3)                  │
│          发送 HTTP 请求，携带 JWT Token           │
└──────────────────────┬──────────────────────────┘
                       │ HTTP / HTTPS
┌──────────────────────▼──────────────────────────┐
│              Spring Security Filter Chain         │
│  ┌─────────────────────────────────────────────┐ │
│  │  JwtAuthenticationFilter                    │ │
│  │  1. 提取 Authorization 头                   │ │
│  │  2. 去除 Bearer 前缀                        │ │
│  │  3. 验证 Token 有效性                       │ │
│  │  4. 解析 userId + role → SecurityContext    │ │
│  └─────────────────────────────────────────────┘ │
└──────────────────────┬──────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────┐
│              Controller 层 (接口层)               │
│  接收请求，参数校验，调用 Service，返回 Result<T> │
└──────────────────────┬──────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────┐
│              Service 层 (业务逻辑层)              │
│  核心业务逻辑，缓存策略，分布式锁，布隆过滤器     │
└──────────────────────┬──────────────────────────┘
                       │
┌──────────┬───────────▼────────────┬──────────────┐
│ MySQL    │        Redis           │  Redisson    │
│ 持久化   │   缓存/分布式锁/空值      │  布隆过滤器   │
└──────────┴────────────────────────┴──────────────┘
```

### 分层职责表

| 层级 | 包路径 | 职责 | 关键类 |
|------|--------|------|--------|
| Entity | `entity` | 数据模型，与数据库表映射 | User, Article, Category, Comment, Notice, SiteConfig |
| Repository | `repository` | 数据访问，继承 JpaRepository | UserRepository, ArticleRepository 等 |
| Service | `service/impl` | 业务逻辑，事务管理 | UserServiceImpl, ArticleServiceImpl 等 |
| Controller | `controller` | 接口定义，权限控制 | UserController, ArticleController 等 |
| Common | `common` | 工具类，统一封装 | Result, JwtUtil, RedisUtil, BloomFilterUtil, UserPrincipal |
| Config | `config` | 安全与中间件配置 | SecurityConfig, JwtAuthenticationFilter, RedisConfig, RedissonConfig |

---

## 2. 第一步：基础 CRUD 搭建

### 思路

从最简单的 CRUD 开始，用 Spring Data JPA 自动生成 SQL，快速搭建数据模型和接口。

### 实体模型

```java
@Entity
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private LocalDateTime createTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    public enum Role { USER, ADMIN }
}
```

### Repository 层

```java
public interface ArticleRepository extends JpaRepository<Article, Long> {
    // Spring Data JPA 自动生成 SQL
    List<Article> findByUserId(Long userId);
    Page<Article> findByCategoryId(Long categoryId, Pageable pageable);

    // 自定义 JPQL 查询
    @Query("SELECT a FROM Article a WHERE " +
           "(:categoryId IS NULL OR a.categoryId = :categoryId) AND " +
           "(:keyword IS NULL OR a.title LIKE %:keyword%)")  
    Page<Article> findByFilter(@Param("categoryId") Long categoryId,
                               @Param("keyword") String keyword,
                               Pageable pageable);
}
```

### 优化经验

| 问题 | 优化方式 |
|------|----------|
| 分页查询全表扫描 | 用 Spring Data 的 `Pageable` 实现，JPA 自动加 `LIMIT` |
| 按分类+关键词组合查询 | 用 `@Query` 写 JPQL，避免写多个查询方法 |
| 密码明文存储 | 引入 BCrypt 加密 |

---

## 3. 第二步：统一响应与异常处理

### 思路

所有接口返回统一格式，前端不需要针对不同接口写不同的解析逻辑。

### Result 统一响应(下面这个有很多问题)

```java
public class Result<T> {
    private Integer code;     // 200=成功, 401=未登录, 403=无权限, 500=异常
    private String message;   // 提示信息
    private T data;           // 业务数据

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
```

### GlobalExceptionHandler 全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(RuntimeException.class)
    public Result<String> handleRuntimeException(RuntimeException e) {
        return Result.error(e.getMessage());
    }
}
```

### 优化经验

| 问题 | 优化方式 |
|------|----------|
| 每个接口返回格式不统一 | 封装 `Result<T>` 泛型类 |
| 每个 Controller 里重复写 try-catch | `@RestControllerAdvice` 全局拦截 |
| 异常信息直接暴露堆栈 | 只返回 message，不暴露内部细节 |

---

## 4. 第三步：Spring Security + JWT 认证

### 思路

从自定义拦截器升级为 Spring Security 标准 Filter Chain，实现无状态 JWT 认证。

### 认证流程图

```
┌────────┐    ① POST /api/user/login     ┌──────────┐
│  前端  │ ──────────────────────────────► │Controller│
│        │ ◄────────────────────────────── │          │
│        │    ② 返回 { user, token }       └──────────┘
└──┬─────┘
   │ ③ 后续请求携带 Authorization: Bearer <token>
   ▼
┌──────────────────────────────────────────────────────┐
│            JwtAuthenticationFilter                    │
│                                                       │
│  ④ 提取 token → ⑤ 验证签名 → ⑥ 解析 userId + role  │
│                                                       │
│  ⑦ 构造 UserPrincipal → ⑧ 写入 SecurityContext      │
└──────────────────────┬───────────────────────────────┘
                       │
              ⑨ 交给 Spring Security 判断权限
                       │
         ┌─────────────▼─────────────┐
         │  有权限 → 放行到 Controller │
         │  无权限 → 403 Forbidden    │
         │  未登录 → 401 Unauthorized │
         └───────────────────────────┘
```

### 关键代码

**UserPrincipal** — 桥接 	JWT 和 Spring Security 的核心：

```java
@Getter
public class UserPrincipal implements UserDetails {
    private final Long userId;
    private final String role;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // ROLE_ 前缀是 Spring Security 约定，hasRole() 会自动拼前缀
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getUsername() { return userId.toString(); }
    // ... 其他方法默认返回 true
}
```

**JwtAuthenticationFilter** — 每次请求都走这个过滤器：

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(...) {
        String token = request.getHeader("Authorization");

        // Bearer 前缀处理
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }

        // 1. 没有 token → 放行（交给 Security 判断是否需要登录）
        if (token == null || token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. token 无效 → 清除上下文，放行
        if (!jwtUtil.validateToken(token)) {
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        // 3. token 有效 → 解析 userId + role，构造认证信息
        Long userId = jwtUtil.getUserIdFromToken(token);
        String role = jwtUtil.getRoleFromToken(token);
        UserPrincipal principal = new UserPrincipal(userId, role);

        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);

        // 4. 放行
        filterChain.doFilter(request, response);
    }
}
```

**SecurityConfig** — 安全策略总控：

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // 启用 @PreAuthorize
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())                              // 前后端分离不需要 CSRF
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS)) // 无状态
            .httpBasic(b -> b.disable())
            .formLogin(f -> f.disable())

            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/user/login", "/api/user/register").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/article/list", ...).permitAll()
                .anyRequest().authenticated()
            )

            .exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

### 优化经验

| 问题 | 优化方式 |
|------|----------|
| 自定义拦截器无法与 Spring Security 深度集成 | 改用 `OncePerRequestFilter` |
| 前端传 `Bearer ` 前缀导致 token 解析失败 | `token.substring(7).trim()` |
| userId 可伪造的水平越权 | 用 `@AuthenticationPrincipal UserPrincipal` 从 SecurityContext 取 |
| 字段注入不安全 | 全部改构造器注入，保证依赖不可变 |

---

## 5. 第四步：角色权限系统

### 思路

在 JWT 鉴权（你是谁）基础上加授权（你能做什么），用 Spring Security 的 `@PreAuthorize` 实现方法级权限控制。

### 权限模型

```
┌────────────────────────────────────────────────┐
│                  User 实体                       │
│  id=1, username=admin, role=ADMIN              │
│  id=2, username=test1, role=USER               │
└─────────────────┬──────────────────────────────┘
                  │ 登录
                  ▼
┌────────────────────────────────────────────────┐
│              JWT Token Payload                  │
│  { "sub": "1", "role": "ADMIN", "exp": ... }   │
└─────────────────┬──────────────────────────────┘
                  │ 请求
                  ▼
┌────────────────────────────────────────────────┐
│           UserPrincipal.getAuthorities()        │
│  ADMIN → [ SimpleGrantedAuthority("ROLE_ADMIN") ] │
│  USER  → [ SimpleGrantedAuthority("ROLE_USER")  ] │
└─────────────────┬──────────────────────────────┘
                  │ 匹配
                  ▼
┌─────────────────────────────────────────────────┐
│           @PreAuthorize("hasRole('ADMIN')")      │
│  hasRole('ADMIN') → 自动拼前缀 → 匹配 ROLE_ADMIN │
│  匹配成功 → 放行    匹配失败 → 403 Forbidden     │
└─────────────────────────────────────────────────┘
```

### 关键代码

**枚举设计** — 数据库存简短值，转换层加前缀：

```java
// 数据库存: "USER" / "ADMIN"（干净）
public enum Role { USER, ADMIN }

// getAuthorities() 加前缀: "ROLE_USER" / "ROLE_ADMIN"（Spring Security 约定）
return List.of(new SimpleGrantedAuthority("ROLE_" + role));
```

**前后端双重防护**：

| 防护层 | 作用 | 绕过难度 |
|--------|------|----------|
| 前端路由守卫 `requiresAdmin` | 普通用户看不到入口 | 容易（手动输 URL） |
| 后端 `@PreAuthorize` | 即使绕过前端，服务端拒绝 | 极难（需要伪造 JWT 签名） |

### 优化经验

| 问题 | 优化方式 |
|------|----------|
| 枚举名 `ROLE_ADMIN` 语义重复 | 枚举用 `ADMIN`，转换层加 `ROLE_` 前缀 |
| 所有登录用户都能改网站配置 | saveConfig/deleteConfig 加 `@PreAuthorize("hasRole('ADMIN')")` |
| 前端不区分角色，所有人看到同样界面 | `isAdmin` 计算属性 + 路由守卫 + 组件级校验 |

---

## 6. 第五步：Redis 缓存优化

### 思路

从「每次请求都查数据库」优化为「先查缓存，缓存没有再查数据库并回填」，同时解决缓存三大问题。

### 缓存策略：Cache-Aside 模式

```
┌────────┐    ① 查询请求     ┌─────────┐
│ 客户端 │ ────────────────► │ Service │
└────────┘                   └────┬────┘
                                  │ ② 先查 Redis
                                  ▼
                           ┌─────────────┐
                           │    Redis     │
                           └──────┬──────┘
                          命中 ↓     │ 未命中
                          返回 ↓     ▼
                          缓存   ┌─────────┐
                          数据   │  MySQL   │
                                 └────┬────┘
                                      │ ③ 查数据库
                                      ▼
                                 ④ 写入 Redis（随机过期时间）
                                      │
                                      ▼
                                 ⑤ 返回数据
```

### 缓存三大问题解决方案

| 问题 | 场景 | 解决方案 | 代码体现 |
|------|------|----------|----------|
| **缓存穿透** | 查询不存在的数据，绕过缓存直接打数据库 | 布隆过滤器 + 空值缓存 | `BloomFilterUtil.mightContain()` + `redisUtil.set(key, "NULL", 300)` |
| **缓存击穿** | 热点 key 过期瞬间，大量请求同时打到数据库 | 分布式锁 + 双重检查（DCL） | `redisUtil.tryLock()` + 二次查缓存 |
| **缓存雪崩** | 大量 key 同时过期 | 随机过期时间 | `3600 + random.nextInt(1800)` |

### 缓存击穿的 DCL（Double-Check Locking）流程

```
┌──────────┐
│  请求进来 │
└─────┬────┘
      ▼
┌──────────────┐     命中      ┌─────────┐
│ ① 查 Redis   │ ──────────────► │ 返回缓存 │
└──────┬───────┘                └─────────┘
       │ 未命中
       ▼
┌──────────────┐     获取失败   ┌──────────────┐
│ ② 获取分布式锁 │ ──────────────► │ 等待重试/返回  │
└──────┬───────┘                └──────────────┘
       │ 获取成功
       ▼
┌──────────────┐     命中      ┌─────────┐
│ ③ 再次查 Redis │ ──────────────► │ 返回缓存 │  ← DCL 核心
└──────┬───────┘                └─────────┘
       │ 未命中
       ▼
┌──────────────┐
│ ④ 查 MySQL    │
│ ⑤ 写入 Redis  │
│ ⑥ 释放锁      │
└──────────────┘
```

### 关键代码 — ArticleServiceImpl.getArticle

```java
public Article getArticle(Long id) {
    // 第一层防护：布隆过滤器
    if (!bloomFilterUtil.mightContain(id)) {
        throw new RuntimeException("文章不存在");
    }

    String key = "article:" + id;

    // 第一次查缓存
    String cached = redisUtil.get(key);
    if (cached != null) {
        if ("NULL".equals(cached)) return null;  // 空值缓存
        return JSON.parseObject(cached, Article.class);
    }

    // 缓存未命中 → 获取分布式锁
    String lockKey = "lock:article:" + id;
    String lockValue = UUID.randomUUID().toString();
    boolean locked = redisUtil.tryLock(lockKey, lockValue, 30);

    if (!locked) {
        // 没拿到锁，短暂等待后重试
        try { Thread.sleep(100); } catch (InterruptedException e) { }
        return getArticle(id);
    }

    try {
        // DCL：拿到锁后再查一次缓存
        cached = redisUtil.get(key);
        if (cached != null) {
            if ("NULL".equals(cached)) return null;
            return JSON.parseObject(cached, Article.class);
        }

        // 查数据库
        Article article = articleRepository.findById(id).orElse(null);

        if (article != null) {
            // 随机过期时间，防止雪崩
            int expire = 3600 + new Random().nextInt(1800);
            redisUtil.set(key, JSON.toJSONString(article), expire);
        } else {
            // 空值缓存，防止穿透
            redisUtil.set(key, "NULL", 300);
        }
        return article;
    } finally {
        redisUtil.unlock(lockKey, lockValue);
    }
}
```

### 优化经验

| 问题 | 优化方式 |
|------|----------|
| 高并发下缓存击穿 | 分布式锁 + DCL，只让一个请求查数据库 |
| 大量 key 同时过期 | 过期时间加随机值 `3600 + random(1800)` |
| 恶意查询不存在的 ID | 布隆过滤器前置拦截 + 空值缓存兜底 |
| 分布式锁死锁 | 用 `try-finally` 保证锁释放，锁值用 UUID 防误删 |

---

## 7. 第六步：布隆过滤器防缓存穿透

### 思路

在缓存之前加一层布隆过滤器，用极小的内存判断「这个 ID 可能存在」还是「这个 ID 一定不存在」。

### 布隆过滤器原理

```
                    位数组（Bit Array）
              ┌───┬───┬───┬───┬───┬───┬───┬───┐
              │ 0 │ 1 │ 0 │ 1 │ 0 │ 1 │ 0 │ 0 │
              └───┴───┴───┴───┴───┴───┴───┴───┘
                      ▲       ▲   ▲
                      │       │   │
           ID=5 的哈希映射 ────┘   │
           ID=5 的另一哈希映射 ────┘

  判断逻辑：
  ┌─────────────┐     所有位都是1     ┌──────────────────┐
  │ mightContain │ ───────────────────► │ 可能存在（有误判率）│
  └──────┬──────┘                      └──────────────────┘
         │ 有位是0
         ▼
  ┌──────────────┐
  │  一定不存在   │  ← 这个是 100% 准确的
  └──────────────┘
```

### BloomFilterUtil 关键代码

```java
@Component
public class BloomFilterUtil {
    private RBloomFilter<Long> bloomFilter;

    @PostConstruct
    public void init() {
        bloomFilter = redissonClient.getBloomFilter("articleBloomFilter");
        // 初始化：预期10000个元素，误判率0.01
        if (!bloomFilter.isExists()) {
            boolean success = bloomFilter.tryInit(10000L, 0.01);
            if (!success) {
                throw new RuntimeException("布隆过滤器初始化失败");
            }
        }
        // 启动时全量加载已有文章 ID
        articleRepository.findAll().forEach(a -> bloomFilter.add(a.getId()));
    }

    public boolean mightContain(Long id) {
        return bloomFilter.contains(id);
    }

    public void add(Long id) {
        bloomFilter.add(id);
    }
}
```

### 防穿透的完整请求流程

```
┌─────────┐
│ 请求 ID  │
└────┬────┘
     ▼
┌──────────────────┐     不存在      ┌───────────────────┐
│ ① 布隆过滤器检查  │ ───────────────► │ 直接返回"文章不存在" │
└────┬─────────────┘                 └───────────────────┘
     │ 可能存在
     ▼
┌──────────────────┐     命中      ┌─────────┐
│ ② 查 Redis 缓存  │ ─────────────► │ 返回缓存 │
└────┬─────────────┘               └─────────┘
     │ 未命中
     ▼
┌──────────────────┐     存在      ┌──────────────────┐
│ ③ 查 MySQL       │ ─────────────► │ 写入 Redis，返回  │
└────┬─────────────┘               └──────────────────┘
     │ 不存在
     ▼
┌──────────────────┐
│ ④ 空值缓存 300s  │  ← 二次兜底：即使布隆误判，也不打 DB
└──────────────────┘
```

### 优化经验

| 问题 | 优化方式 |
|------|----------|
| 布隆过滤器初始化失败但应用正常启动 | `@PostConstruct` 里 `throw RuntimeException`，Fail-Fast |
| 硬编码容量和误判率 | 用 `@Value` 或 getter 暴露，方便 Controller 监控 |
| 新增文章后布隆过滤器没更新 | `ArticleServiceImpl.createArticle` 里调用 `bloomFilterUtil.add()` |
| 布隆过滤器需要扩容 | 提供 `rebuild(newCapacity)` 方法 + 管理员接口 |

---

## 8. 技术栈与版本

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 运行时 |
| Spring Boot | 4.1.0 | 应用框架 |
| Spring Security | 6.x | 认证与授权 |
| Spring Data JPA | 3.x | ORM 与数据访问 |
| MySQL | 8.0 | 关系型数据库 |
| Redis | 7.x | 缓存 / 分布式锁 / 空值缓存 |
| Redisson | 3.27.0 | 布隆过滤器 / 分布式锁 |
| JWT (JJWT) | 0.12.x | Token 生成与验证 |
| BCrypt | — | 密码加密 |
| Lombok | — | 减少样板代码 |
| Vue 3 | 3.5 | 前端框架 |
| Vite | 8.1.1 | 前端构建工具 |

---

## 总结：从 CRUD 到企业级的优化路径

```
  纯 CRUD              统一封装              认证授权             缓存优化            前置防护
┌─────────┐       ┌─────────────┐      ┌──────────────┐    ┌──────────────┐   ┌──────────────┐
│ 直接查库  │  ►   │ Result<T>   │  ►   │ JWT + Spring │ ►  │ Redis 缓存   │ ► │ 布隆过滤器   │
│ 无统一格式│       │ 全局异常处理 │      │ Security     │    │ 分布式锁     │   │ Fail-Fast   │
│ 密码明文  │       │ BCrypt 加密  │      │ 角色权限系统  │    │ DCL 防击穿   │   │ 双层防穿透   │
└─────────┘       └─────────────┘      └──────────────┘    └──────────────┘   └──────────────┘
   第一步               第二步                第三步              第四步             第五步
```

每一步的核心经验：
1. **CRUD**：用 JPA 快速搭建，不要过早优化
2. **统一封装**：前后端一致的数据格式，是协作的基础
3. **认证授权**：Spring Security Filter Chain 是行业标准，不要自己造轮子
4. **缓存优化**：Cache-Aside + 分布式锁 + 随机过期，三板斧解决三大问题
5. **前置防护**：布隆过滤器 + Fail-Fast，用极小成本拦截无效请求
