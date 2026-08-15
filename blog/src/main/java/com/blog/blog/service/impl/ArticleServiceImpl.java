package com.blog.blog.service.impl;

import com.blog.blog.common.constant.BlogCacheConstants;
import com.blog.blog.common.util.BloomFilterUtil;
import com.blog.blog.common.util.CacheCleaner;
import com.blog.blog.common.util.RedisUtil;
import com.blog.blog.config.RabbitMQConfig;
import com.blog.blog.dto.SummaryGenerateMessage;
import com.blog.blog.entity.Article;
import com.blog.blog.entity.User;
import com.blog.blog.repository.ArticleRepository;
import com.blog.blog.repository.UserRepository;
import com.blog.blog.service.ArticleService;
import com.blog.blog.service.DeepSeekService;
import com.blog.blog.vo.PageResult;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private static final Logger log = LoggerFactory.getLogger(ArticleServiceImpl.class);

    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final RedisUtil redisUtil;
    private final DeepSeekService deepSeekService;
    private final BloomFilterUtil bloomFilterUtil;
    private final RabbitTemplate rabbitTemplate;
    private final CacheCleaner cacheCleaner;
    private final Executor cacheRebuildExecutor;
    private final PlatformTransactionManager transactionManager;
    private TransactionTemplate transactionTemplate;

    @PostConstruct
    public void init() {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public Article createArticle(String title, String content, Long userId, String categoryId) {
        Article article = new Article();
        article.setTitle(title);
        article.setContent(content);
        article.setUserId(userId);
        article.setCategoryId(categoryId);
        article.setCreateTime(LocalDateTime.now());
        article.setUpdateTime(LocalDateTime.now());
        article.setSummary("AI 摘要生成中...");

        article = articleRepository.save(article);
        // 填充作者昵称（返回给前端展示，避免显示兜底"用户"）
        ensureAuthorName(article);
        bloomFilterUtil.add(article.getId());

        // ZSet 排行榜初始化
        redisUtil.zAdd(BlogCacheConstants.ARTICLE_RANKING_ZSET_VIEWS, article.getId(), 0);

        // MQ 异步生成摘要
        try {
            SummaryGenerateMessage msg = new SummaryGenerateMessage();
            msg.setArticleId(article.getId());
            msg.setTimestamp(System.currentTimeMillis());
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SUMMARY_EXCHANGE,
                    RabbitMQConfig.SUMMARY_ROUTING_KEY,
                    msg
            );
        } catch (Exception e) {
            log.warn("发送摘要生成消息失败,articleId={},降级为同步生成", article.getId(), e);
            try {
                String syncSummary = deepSeekService.generateSummary(content);
                article.setSummary(syncSummary);
                article = articleRepository.save(article);
            } catch (Exception ex) {
                log.error("同步摘要生成也失败,articleId={}", article.getId(), ex);
            }
        }

        cacheCleaner.clearArticleRelatedCaches(article.getId());
        return article;
    }

    @Override
    public PageResult<Article> getAllArticlesPage(int page, int size, String categoryId, String keyword) {
        if (page < 0) page = 0;
        if (size <= 0) size = 10;

        boolean hasCategory = categoryId != null && !categoryId.isEmpty();
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        String kw = hasKeyword ? keyword.trim() : null;

        String listKey = BlogCacheConstants.ARTICLE_LIST_PAGE_KEY + page
                + (hasCategory ? ":cat:" + categoryId : "")
                + (hasKeyword ? ":kw:" + kw : "");
        String totalKey = BlogCacheConstants.ARTICLE_TOTAL_COUNT_KEY
                + (hasCategory ? ":cat:" + categoryId : "")
                + (hasKeyword ? ":kw:" + kw : "");

        Object cacheObj = redisUtil.get(listKey);
        Object totalObj = redisUtil.get(totalKey);

        PageResult<Article> result;

        if (cacheObj instanceof List && totalObj instanceof Number) {
            @SuppressWarnings("unchecked")
            List<Article> cachedList = (List<Article>) cacheObj;
            long total = ((Number) totalObj).longValue();
            // 旧缓存可能缺 authorName（实体新增字段前写入），补齐后回写缓存
            if (!cachedList.isEmpty() && cachedList.get(0).getAuthorName() == null) {
                fillAuthorNames(cachedList);
                redisUtil.set(listKey, cachedList, BlogCacheConstants.ARTICLE_LIST_EXPIRE);
            }
            result = PageResult.of(cachedList, total, page, size);
        } else {
            Pageable pageable = PageRequest.of(page, size);
            Page<Article> pageResult;

            if (hasKeyword && hasCategory) {
                pageResult = articleRepository
                        .searchByCategoryAndKeyword(categoryId, kw, pageable);
            } else if (hasKeyword) {
                pageResult = articleRepository
                        .findByTitleContainingOrContentContainingOrderByCreateTimeDesc(kw, kw, pageable);
            } else if (hasCategory) {
                pageResult = articleRepository.findByCategoryIdOrderByCreateTimeDesc(categoryId, pageable);
            } else {
                pageResult = articleRepository.findAllByOrderByCreateTimeDesc(pageable);
            }

            long expire = BlogCacheConstants.ARTICLE_LIST_EXPIRE + ThreadLocalRandom.current().nextInt(120);

            if (pageResult.hasContent()) {
                // 先填充作者昵称，再写缓存（保证缓存里带 authorName）
                fillAuthorNames(pageResult.getContent());
                redisUtil.set(listKey, pageResult.getContent(), expire);
                redisUtil.set(totalKey, pageResult.getTotalElements(),
                        BlogCacheConstants.ARTICLE_TOTAL_EXPIRE + ThreadLocalRandom.current().nextInt(120));
            } else {
                redisUtil.set(listKey, Collections.emptyList(), BlogCacheConstants.NULL_CACHE_EXPIRE);
                redisUtil.set(totalKey, 0L, BlogCacheConstants.NULL_CACHE_EXPIRE);
            }

            result = PageResult.of(pageResult);
        }

        // 用 Redis 实时点赞数覆盖 DB 中的旧数据（点赞数异步同步到 DB，Redis 才是最新值）
        enrichLikeCounts(result.getContent());
        return result;
    }

    /**
     * 从 Redis 获取实时点赞数，覆盖文章列表中的 likeCount（DB 数据可能滞后）
     */
    private void enrichLikeCounts(List<Article> articles) {
        if (articles == null || articles.isEmpty()) return;
        for (Article article : articles) {
            try {
                Object countObj = redisUtil.get(BlogCacheConstants.ARTICLE_LIKE_COUNT_KEY + article.getId());
                if (countObj instanceof Number) {
                    article.setLikeCount(((Number) countObj).intValue());
                }
            } catch (Exception e) {
                // Redis 不可用时保留 DB 原始值，不阻塞列表加载
            }
        }
    }

    /**
     * 批量填充文章作者昵称（一次查 user 表，避免 N+1 查询）
     */
    private void fillAuthorNames(List<Article> articles) {
        if (articles == null || articles.isEmpty()) return;
        Set<Long> userIds = articles.stream()
                .map(Article::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) return;
        Map<Long, String> nicknameMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname, (a, b) -> a));
        articles.forEach(a -> a.setAuthorName(nicknameMap.getOrDefault(a.getUserId(), "用户")));
    }

    /**
     * 单篇文章作者昵称补齐（缓存命中时判断缺失才填充，避免重复查库）
     */
    private void ensureAuthorName(Article article) {
        if (article == null || article.getAuthorName() != null) return;
        fillAuthorNames(Collections.singletonList(article));
    }

    @Override
    public Article getArticle(Long id) {
        if (!bloomFilterUtil.mightContain(id)) {
            throw new RuntimeException("文章不存在");
        }

        String key = BlogCacheConstants.ARTICLE_DETAIL_KEY + id;

        // 1. 逻辑过期缓存读取
        Object cacheObj = redisUtil.getWithLogicalExpire(key);

        if (cacheObj != null && !"LOGICAL_EXPIRED".equals(cacheObj)) {
            if ("NULL".equals(cacheObj)) throw new RuntimeException("文章不存在");
            Article cached = (Article) cacheObj;
            ensureAuthorName(cached);   // 旧缓存可能缺 authorName，补齐
            return cached;
        }

        // 逻辑过期 → 异步重建 + 返回旧值
        if ("LOGICAL_EXPIRED".equals(cacheObj)) {
            cacheRebuildExecutor.execute(() -> {
                try {
                    rebuildArticleDetailCache(id);
                } catch (Exception e) {
                    log.error("异步重建文章缓存失败, articleId={}", id, e);
                }
            });
            Object rawCache = redisUtil.get(key);
            if (rawCache instanceof Map) {
                Map<?, ?> wrapper = (Map<?, ?>) rawCache;
                Object data = wrapper.get("data");
                if (data instanceof Article) {
                    Article cached = (Article) data;
                    ensureAuthorName(cached);
                    return cached;
                }
            }
        }

        // 2. 未命中 → 分布式锁防击穿
        String lockKey = BlogCacheConstants.ARTICLE_LOCK_KEY + id;
        RLock rLock = redisUtil.getLock(lockKey);
        boolean locked = false;
        try {
            locked = rLock.tryLock(5, -1, TimeUnit.SECONDS);
            if (locked) {
                cacheObj = redisUtil.getWithLogicalExpire(key);
                if (cacheObj != null && !"LOGICAL_EXPIRED".equals(cacheObj)) {
                    if ("NULL".equals(cacheObj)) throw new RuntimeException("文章不存在");
                    Article cached = (Article) cacheObj;
                    ensureAuthorName(cached);
                    return cached;
                }

                java.util.Optional<Article> articleOpt = articleRepository.findById(id);
                if (articleOpt.isEmpty()) {
                    redisUtil.set(key, "NULL", BlogCacheConstants.NULL_CACHE_EXPIRE);
                    throw new RuntimeException("文章不存在");
                }

                Article article = articleOpt.get();
                ensureAuthorName(article);   // 填充后再写缓存
                long expire = BlogCacheConstants.ARTICLE_DETAIL_EXPIRE + ThreadLocalRandom.current().nextInt(300);
                redisUtil.setWithLogicalExpire(key, article, expire);
                return article;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (locked && rLock.isHeldByCurrentThread()) {
                rLock.unlock();
            }
        }

        Article fallback = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文章不存在"));
        ensureAuthorName(fallback);
        return fallback;
    }

    private void rebuildArticleDetailCache(Long id) {
        String rebuildLockKey = BlogCacheConstants.REBUILD_LOCK_PREFIX + id;
        RLock rebuildLock = redisUtil.getLock(rebuildLockKey);
        boolean locked = false;
        try {
            locked = rebuildLock.tryLock(3, TimeUnit.SECONDS);
            if (!locked) return;

            Object cached = redisUtil.getWithLogicalExpire(BlogCacheConstants.ARTICLE_DETAIL_KEY + id);
            if (cached != null && !"LOGICAL_EXPIRED".equals(cached)) return;

            Article article = articleRepository.findById(id).orElse(null);
            if (article != null) {
                ensureAuthorName(article);   // 填充后再写缓存
                long expire = BlogCacheConstants.ARTICLE_DETAIL_EXPIRE + ThreadLocalRandom.current().nextInt(300);
                redisUtil.setWithLogicalExpire(BlogCacheConstants.ARTICLE_DETAIL_KEY + id, article, expire);
            } else {
                redisUtil.set(BlogCacheConstants.ARTICLE_DETAIL_KEY + id, "NULL", BlogCacheConstants.NULL_CACHE_EXPIRE);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (locked && rebuildLock.isHeldByCurrentThread()) {
                rebuildLock.unlock();
            }
        }
    }

    @Override
    public List<Article> getAllArticles() {
        Object cacheObj = redisUtil.get(BlogCacheConstants.ARTICLE_LIST_KEY);
        if (cacheObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Article> cached = (List<Article>) cacheObj;
            // 旧缓存可能缺 authorName，补齐
            if (!cached.isEmpty() && cached.get(0).getAuthorName() == null) {
                fillAuthorNames(cached);
            }
            return cached;
        }

        List<Article> articles = articleRepository.findAllByOrderByCreateTimeDesc();

        if (articles.isEmpty()) {
            redisUtil.set(BlogCacheConstants.ARTICLE_LIST_KEY, Collections.emptyList(), BlogCacheConstants.NULL_CACHE_EXPIRE);
        } else {
            fillAuthorNames(articles);   // 先填充再写缓存
            redisUtil.set(BlogCacheConstants.ARTICLE_LIST_KEY, articles,
                    BlogCacheConstants.ARTICLE_LIST_EXPIRE + ThreadLocalRandom.current().nextInt(120));
        }
        return articles;
    }

    @Override
    public List<Article> getArticlesByUserId(Long userId) {
        List<Article> articles = articleRepository.findByUserIdOrderByCreateTimeDesc(userId);
        fillAuthorNames(articles);
        return articles;
    }

    @Override
    public Article updateArticle(Long id, String title, String content, String categoryId) {
        Article article = articleRepository.findById(id).orElseThrow(() -> new RuntimeException("文章不存在"));
        article.setTitle(title);
        article.setContent(content);
        if (categoryId != null && !categoryId.isEmpty()) {
            article.setCategoryId(categoryId);
        }
        article.setUpdateTime(LocalDateTime.now());
        article = articleRepository.save(article);
        // 填充作者昵称（返回给前端展示）
        ensureAuthorName(article);

        cacheCleaner.clearArticleRelatedCaches(article.getId());
        redisUtil.zAdd(BlogCacheConstants.ARTICLE_RANKING_ZSET_VIEWS, id, article.getViewCount());

        return article;
    }

    @Override
    public void deleteArticle(Long id) {
        Article article = articleRepository.findById(id).orElseThrow(() -> new RuntimeException("文章不存在"));
        articleRepository.delete(article);

        cacheCleaner.clearArticleRelatedCaches(article.getId());
        redisUtil.zRem(BlogCacheConstants.ARTICLE_RANKING_ZSET_VIEWS, id);
    }

    @Override
    public List<Article> getArticleRankingByViews() {
        Set<ZSetOperations.TypedTuple<Object>> topSet =
                redisUtil.zReverseRangeWithScores(BlogCacheConstants.ARTICLE_RANKING_ZSET_VIEWS, 0, 9);

        if (topSet != null && !topSet.isEmpty()) {
            List<Long> articleIds = topSet.stream()
                    .map(t -> {
                        Object val = t.getValue();
                        if (val instanceof Number) {
                            return ((Number) val).longValue();
                        } else if (val != null) {
                            try { return Long.parseLong(val.toString()); } catch (Exception ignored) {}
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            List<Long> missIds = new ArrayList<>();
            Map<Long, Article> cacheMap = new HashMap<>();
            for (Long aid : articleIds) {
                Object cacheObj = redisUtil.getWithLogicalExpire(BlogCacheConstants.ARTICLE_DETAIL_KEY + aid);

                if (cacheObj == null || "NULL".equals(cacheObj)) {
                    missIds.add(aid);
                    continue;
                }
                if ("LOGICAL_EXPIRED".equals(cacheObj)) {
                    Article a = extractArticleFromLogicalCache(aid);
                    if (a != null) {
                        cacheMap.put(aid, a);
                    } else {
                        missIds.add(aid);
                    }
                    continue;
                }
                if (cacheObj instanceof Article) {
                    cacheMap.put(aid, (Article) cacheObj);
                } else {
                    missIds.add(aid);
                }
            }

            if (!missIds.isEmpty()) {
                List<Article> dbList = articleRepository.findAllById(missIds);
                for (Article a : dbList) {
                    ensureAuthorName(a);   // 填充后再写缓存
                    long expire = BlogCacheConstants.ARTICLE_DETAIL_EXPIRE + ThreadLocalRandom.current().nextInt(300);
                    redisUtil.setWithLogicalExpire(BlogCacheConstants.ARTICLE_DETAIL_KEY + a.getId(), a, expire);
                    cacheMap.put(a.getId(), a);
                }
            }

            List<Article> ranked = articleIds.stream()
                    .map(cacheMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            fillAuthorNames(ranked);   // 缓存里的文章可能缺 authorName，统一补齐
            return ranked;
        }

        List<Article> articles = articleRepository.findTop10ByOrderByViewCountDesc();
        if (!articles.isEmpty()) {
            fillAuthorNames(articles);   // 先填充
            for (Article a : articles) {
                Object incObj = redisUtil.get(BlogCacheConstants.ARTICLE_VIEW_KEY + a.getId());
                long inc = 0L;
                if (incObj instanceof Number) {
                    inc = ((Number) incObj).longValue();
                } else if (incObj != null) {
                    try { inc = Long.parseLong(incObj.toString()); } catch (Exception ignored) {}
                }
                double score = a.getViewCount() + inc;
                redisUtil.zAdd(BlogCacheConstants.ARTICLE_RANKING_ZSET_VIEWS, a.getId(), score);
            }
            for (Article a : articles) {
                long expire = BlogCacheConstants.ARTICLE_DETAIL_EXPIRE + ThreadLocalRandom.current().nextInt(300);
                redisUtil.setWithLogicalExpire(BlogCacheConstants.ARTICLE_DETAIL_KEY + a.getId(), a, expire);
            }
        }
        return articles;
    }

    private Article extractArticleFromLogicalCache(Long articleId) {
        Object rawCache = redisUtil.get(BlogCacheConstants.ARTICLE_DETAIL_KEY + articleId);
        if (rawCache instanceof Map) {
            Map<?, ?> wrapper = (Map<?, ?>) rawCache;
            Object data = wrapper.get("data");
            if (data instanceof Article) {
                return (Article) data;
            }
        }
        return null;
    }

    @Override
    public List<Article> getArticleRankingByLatest() {
        Object cacheObj = redisUtil.get(BlogCacheConstants.ARTICLE_RANKING_LATEST_KEY);
        if (cacheObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Article> cached = (List<Article>) cacheObj;
            // 旧缓存可能缺 authorName，补齐
            if (!cached.isEmpty() && cached.get(0).getAuthorName() == null) {
                fillAuthorNames(cached);
            }
            return cached;
        }

        List<Article> articles = articleRepository.findTop10ByOrderByCreateTimeDesc();

        if (articles.isEmpty()) {
            redisUtil.set(BlogCacheConstants.ARTICLE_RANKING_LATEST_KEY, Collections.emptyList(), BlogCacheConstants.NULL_CACHE_EXPIRE);
        } else {
            fillAuthorNames(articles);   // 先填充再写缓存
            redisUtil.set(BlogCacheConstants.ARTICLE_RANKING_LATEST_KEY, articles,
                    BlogCacheConstants.ARTICLE_RANKING_EXPIRE + ThreadLocalRandom.current().nextInt(60));
        }
        return articles;
    }

    @Override
    public void incrementViewCount(Long articleId) {
        redisUtil.increment(BlogCacheConstants.ARTICLE_VIEW_KEY + articleId);
        redisUtil.sadd(BlogCacheConstants.ARTICLE_VIEW_DIRTY_KEY, articleId);
        redisUtil.zIncrBy(BlogCacheConstants.ARTICLE_RANKING_ZSET_VIEWS, articleId, 1);
    }

    @Override
    @Scheduled(initialDelay = 60_000, fixedRate = 600_000)
    public void syncViewCountToDB() {
        String lockKey = BlogCacheConstants.VIEW_SYNC_LOCK;
        RLock lock = redisUtil.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(5, -1, TimeUnit.SECONDS);
            if (!locked) {
                log.info("上次阅读量同步尚未完成，跳过本次");
                return;
            }

            Set<Object> dirtyIds = redisUtil.smembers(BlogCacheConstants.ARTICLE_VIEW_DIRTY_KEY);
            if (dirtyIds == null || dirtyIds.isEmpty()) return;

            log.info("开始同步阅读量，脏文章数量：{}", dirtyIds.size());
            long start = System.currentTimeMillis();
            int successCount = 0;
            int failCount = 0;

            for (Object idObj : dirtyIds) {
                try {
                    Long id = Long.valueOf(idObj.toString());
                    Long delta = redisUtil.getAndReset(BlogCacheConstants.ARTICLE_VIEW_KEY + id);
                    if (delta != null && delta > 0) {
                        try {
                            transactionTemplate.execute(status -> {
                                articleRepository.incrementViewCount(id, delta.intValue());
                                return null;
                            });
                        } catch (Exception e) {
                            // 同步失败：恢复 delta，防止数据丢失，下次重试可继续同步
                            redisUtil.incrementBy(BlogCacheConstants.ARTICLE_VIEW_KEY + id, delta);
                            throw e;
                        }

                        Article updated = articleRepository.findById(id).orElse(null);
                        if (updated != null) {
                            redisUtil.zAdd(BlogCacheConstants.ARTICLE_RANKING_ZSET_VIEWS, id, updated.getViewCount());
                        }
                        successCount++;
                    }
                    // 【关键修复】逐个移除已同步的脏标记，不直接 del 整个集合
                    // 避免同步过程中新产生的脏标记被误删
                    redisUtil.srem(BlogCacheConstants.ARTICLE_VIEW_DIRTY_KEY, idObj);
                } catch (Exception e) {
                    failCount++;
                    log.error("同步阅读量失败，文章ID：{}", idObj, e);
                }
            }

            log.info("同步阅读量完成，成功：{}，失败：{}，耗时：{}ms",
                    successCount, failCount, System.currentTimeMillis() - start);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Scheduled(initialDelay = 120_000, fixedRate = 600_000)
    @Transactional(rollbackFor = Exception.class) // 写操作必须加事务，解决No active transaction报错
    public void syncLikeCountToDB() {
        String lockKey = BlogCacheConstants.LIKE_SYNC_LOCK;
        RLock lock = redisUtil.getLock(lockKey);
        boolean locked = false;
        try {
            // 尝试获取分布式锁，5秒等待，永久持有，自动续期
            locked = lock.tryLock(5, -1, TimeUnit.SECONDS);
            if (!locked) {
                log.info("点赞同步任务正在执行，跳过本次定时任务");
                return;
            }

            // 获取所有需要同步的文章脏ID集合
            Set<Object> dirtyIdSet = redisUtil.smembers(BlogCacheConstants.ARTICLE_LIKE_DIRTY_KEY);
            if (Objects.isNull(dirtyIdSet) || dirtyIdSet.isEmpty()) {
                log.info("暂无需要同步的点赞数据，直接结束");
                return;
            }

            log.info("开始同步文章点赞数据，待同步文章数量：{}", dirtyIdSet.size());
            int success = 0;
            int fail = 0;

            for (Object idObj : dirtyIdSet) {
                Long articleId;
                try {
                    articleId = Long.valueOf(idObj.toString());
                } catch (NumberFormatException e) {
                    log.error("脏数据ID格式非法，丢弃：{}", idObj);
                    fail++;
                    continue;
                }

                try {
                    // 读取Redis中实时最新点赞总数
                    Object countObj = redisUtil.get(BlogCacheConstants.ARTICLE_LIKE_COUNT_KEY + articleId);
                    int realLikeCount = countObj instanceof Number ? ((Number) countObj).intValue() : 0;

                    // 直接覆盖数据库点赞字段
                    articleRepository.updateLikeCount(articleId, realLikeCount);
                    success++;

                    // 同步成功，移除脏标记
                    redisUtil.srem(BlogCacheConstants.ARTICLE_LIKE_DIRTY_KEY, articleId);
                } catch (Exception e) {
                    log.error("同步点赞数失败，文章ID：{}", articleId, e);
                    fail++;
                }
            }

            log.info("点赞同步任务执行完成，成功：{}，失败：{}", success, fail);
        } catch (InterruptedException e) {
            log.error("点赞同步任务线程中断", e);
            Thread.currentThread().interrupt();
        } finally {
            // 仅当前线程持有锁才释放，防止误删其他线程锁
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}