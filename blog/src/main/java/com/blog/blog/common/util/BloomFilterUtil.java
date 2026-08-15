package com.blog.blog.common.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文章布隆过滤器工具类
 * 作用：缓存穿透的第一道防线，快速判断文章 ID 是否可能存在
 */
@Slf4j
@Component
public class BloomFilterUtil {

    private static final String BLOOM_FILTER_KEY = "blog:article:bloom";
    private static final long EXPECTED_INSERTIONS = 10000L;
    private static final double FALSE_PROBABILITY = 0.01;

    private final RedissonClient redissonClient;
    private final com.blog.blog.repository.ArticleRepository articleRepository;

    public BloomFilterUtil(RedissonClient redissonClient,
                           com.blog.blog.repository.ArticleRepository articleRepository) {
        this.redissonClient = redissonClient;
        this.articleRepository = articleRepository;
    }

    /**
     * 布隆过滤器实例
     * 存储所有文章 ID，用于快速判断 ID 是否存在
     * 初始化失败时项目会启动失败，避免运行时降级
     */
    private RBloomFilter<Long> bloomFilter;

    @PostConstruct
    public void init() {
        try {
            bloomFilter = redissonClient.getBloomFilter(BLOOM_FILTER_KEY);
            bloomFilter.tryInit(EXPECTED_INSERTIONS, FALSE_PROBABILITY);

            if (bloomFilter.count() == 0) {
                List<Long> allIds = articleRepository.findAllIds();
                for (Long id : allIds) {
                    bloomFilter.add(id);
                }
                log.info("布隆过滤器初始化完成，加载文章数量：{}", allIds.size());
            } else {
                log.info("布隆过滤器已存在，跳过加载（当前元素数：{}）", bloomFilter.count());
            }
        } catch (Exception e) {
            log.error("布隆过滤器初始化失败", e);
            throw new RuntimeException("布隆过滤器初始化失败，项目启动终止", e);
        }
    }

    public void add(Long articleId) {
        bloomFilter.add(articleId);
    }

    public boolean mightContain(Long articleId) {
        return bloomFilter.contains(articleId);
    }

    public long getCount() {
        return bloomFilter.count();
    }

    public long getExpectedInsertions() {
        return EXPECTED_INSERTIONS;
    }

    public double getFalseProbability() {
        return FALSE_PROBABILITY;
    }

    public void rebuild(long newExpectedInsertions) {
        try {
            redissonClient.getKeys().delete(BLOOM_FILTER_KEY);
            bloomFilter = redissonClient.getBloomFilter(BLOOM_FILTER_KEY);
            bloomFilter.tryInit(newExpectedInsertions, FALSE_PROBABILITY);

            List<Long> allIds = articleRepository.findAllIds();
            for (Long id : allIds) {
                bloomFilter.add(id);
            }
            log.info("布隆过滤器重建完成，新容量：{}，实际加载：{}", newExpectedInsertions, allIds.size());
        } catch (Exception e) {
            log.error("布隆过滤器重建失败", e);
            throw new RuntimeException("布隆过滤器重建失败", e);
        }
    }
}
