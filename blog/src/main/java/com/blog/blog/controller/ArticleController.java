package com.blog.blog.controller;

import com.blog.blog.common.constant.BlogCacheConstants;
import com.blog.blog.common.result.Result;
import com.blog.blog.config.RabbitMQConfig;
import com.blog.blog.dto.BrowseHistoryMessage;
import com.blog.blog.dto.DelayMessage;
import com.blog.blog.entity.Article;
import com.blog.blog.service.ArticleService;
import com.blog.blog.vo.PageResult;
import org.redisson.api.RLock;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.blog.blog.common.util.RedisUtil;
import com.blog.blog.common.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/article")
public class ArticleController {
    private static final Logger log = LoggerFactory.getLogger(ArticleController.class);
    private final ArticleService articleService;
    private final RedisUtil redisUtil;
    private final RabbitTemplate rabbitTemplate;

    public ArticleController(ArticleService articleService,
                             RedisUtil redisUtil,
                             RabbitTemplate rabbitTemplate) {
        this.articleService = articleService;
        this.redisUtil = redisUtil;
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/create")
    public Result<Article> create(@RequestBody Article article,
                                  @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal.getUserId();   // ← 从 JWT 取，不可伪造

        // 锁 key：同一用户同一时间只能创建一篇文章
        String lockKey = "blog:article:create:lock:" + userId;

        // Redisson 可重入锁(带看门狗续期)
        RLock lock = redisUtil.getLock(lockKey);
        boolean locked = false;
        try {
            // waitTime=等待最多3秒, leaseTime=-1=启用看门狗自动续期
            locked = lock.tryLock(3, -1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.error("系统繁忙,请稍后重试");
        }
        if (!locked) {
            return Result.error("操作太频繁，请稍后重试");
        }
        try {
            Article createdArticle = articleService.createArticle(
                    article.getTitle(), article.getContent(),
                    userId, article.getCategoryId());
            return Result.success(createdArticle);
        } finally {
            // Redisson 锁释放:校验当前线程持有,防止误删别人的锁
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @GetMapping("/{id}")
    public Result<Article> getArticle(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Article article = articleService.getArticle(id);
        articleService.incrementViewCount(id);

        // 用 Redis 实时点赞数覆盖（DB 点赞数由 MQ 异步同步，可能滞后）
        try {
            Object countObj = redisUtil.get(BlogCacheConstants.ARTICLE_LIKE_COUNT_KEY + id);
            if (countObj instanceof Number) {
                article.setLikeCount(((Number) countObj).intValue());
            }
        } catch (Exception e) {
            // Redis 不可用时保留 DB 原始值
        }

        // 登录用户 → 记录浏览历史（Redis/MQ 不可用时降级，不影响文章详情主流程）
        if (principal != null) {
            try {
                Long userId = principal.getUserId();
                String historyKey = BlogCacheConstants.USER_HISTORY_KEY + userId;
                redisUtil.lPush(historyKey, id);
                redisUtil.lTrim(historyKey, 0, BlogCacheConstants.USER_HISTORY_MAX - 1);
                redisUtil.sadd(BlogCacheConstants.USER_HISTORY_DIRTY_KEY, userId);

                BrowseHistoryMessage msg = new BrowseHistoryMessage();
                msg.setUserId(userId);
                msg.setArticleId(id);
                msg.setTimestamp(System.currentTimeMillis());
                rabbitTemplate.convertAndSend(RabbitMQConfig.HISTORY_EXCHANGE,
                        RabbitMQConfig.HISTORY_ROUTING_KEY, msg);
            } catch (Exception e) {
                // 降级：只记录日志，文章详情照常返回（与 createArticle 的 MQ 降级策略一致）
                log.warn("记录浏览历史失败,articleId={},userId={}", id, principal.getUserId(), e);
            }
        }

        return Result.success(article);
    }


    @GetMapping("/list")
    public Result<PageResult<Article>> getAllArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String keyword) {
        PageResult<Article> articles = articleService.getAllArticlesPage(page, size, categoryId, keyword);
        return Result.success(articles);
    }

    @GetMapping("/user/{userId}")
    public Result<List<Article>> getUser(@PathVariable Long userId){
        List<Article> articles=articleService.getArticlesByUserId(userId);
        return Result.success(articles);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")   // 只有管理员能改任何文章
    public Result<Article> updateArticle(@PathVariable Long id, @RequestBody Article article) {
        Article updatedArticle = articleService.updateArticle(
            id, article.getTitle(), article.getContent(), article.getCategoryId());
        return Result.success(updatedArticle);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> deleteArticle(@PathVariable Long id){
        articleService.deleteArticle(id);
        return Result.success("删除成功");
    }

    @GetMapping("/ranking/views")
    public Result<List<Article>> getArticleRankingByViews() {
        List<Article> articles = articleService.getArticleRankingByViews();
        return Result.success(articles);
    }

    @GetMapping("/ranking/latest")
    public Result<List<Article>> getArticleRankingByLatest() {
        List<Article> articles = articleService.getArticleRankingByLatest();
        return Result.success(articles);
    }

    // 文章搜索（语义化入口，内部复用 getAllArticlesPage，避免逻辑重复）
    // 与 GET /list?keyword=xxx 等价，但 /search 语义更清晰，前端搜索场景专用
    @GetMapping("/search")
    public Result<PageResult<Article>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        // 参数校验：空关键词直接返回空页，不发 DB 查询
        if (keyword == null || keyword.trim().isEmpty())    {
            return Result.success(PageResult.empty(page, size));
        }
        // 关键词限长：防止超长字符串拖慢 LIKE 匹配（输入边界校验）
        String kw = keyword.trim();
        if (kw.length() > 50) {
            kw = kw.substring(0, 50);
        }
        // 复用 Service 分页查询（内部已有 Redis 缓存 + 分页 + 分类过滤）
        PageResult<Article> result = articleService.getAllArticlesPage(page, size, null, kw);
        return Result.success(result);
    }

    /**
     * 演示：定时发布文章（延迟队列）
     * 请求 POST /api/article/schedulePublish
     * body: { "articleId": 1, "delaySeconds": 60 }
     * 消息进入 DELAY_QUEUE 等待 TTL 到期后，由 DelayConsumer 处理
     */
    @PostMapping("/schedulePublish")
    public Result<String> schedulePublish(
            @RequestBody Article article,
            @RequestParam(defaultValue = "30") int delaySeconds,
            @AuthenticationPrincipal UserPrincipal principal) {

        // 先保存文章（草稿状态）
        Article saved = articleService.createArticle(
                article.getTitle(), article.getContent(),
                principal.getUserId(), article.getCategoryId());

        // 发送延迟消息：delaySeconds 秒后触发发布
        DelayMessage msg = new DelayMessage();
        msg.setBizType("article_publish");
        msg.setBizId(saved.getId().toString());
        msg.setUserId(principal.getUserId());
        msg.setTimestamp(System.currentTimeMillis());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DELAY_EXCHANGE,
                RabbitMQConfig.DELAY_ROUTING_KEY,
                msg,
                message -> {
                    // 消息级 TTL 覆盖队列默认值，支持不同延迟时长
                    message.getMessageProperties()
                            .setExpiration(String.valueOf(delaySeconds * 1000L));
                    return message;
                });

        return Result.success("文章已创建，将在 " + delaySeconds + " 秒后定时发布");
    }
}
