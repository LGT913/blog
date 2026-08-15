package com.blog.blog.service;

import com.blog.blog.entity.Article;
import com.blog.blog.vo.PageResult;

import java.util.List;

public interface ArticleService {
    Article createArticle(String title, String content, Long userId,String categoryId);
    Article getArticle(Long id);
    List<Article> getAllArticles();
    List<Article> getArticlesByUserId(Long userId);
    Article updateArticle(Long id, String title, String content, String categoryId);
    void deleteArticle(Long id);

    // 获取阅读量排行（按阅读数降序）
    List<Article> getArticleRankingByViews();

    // 获取最新发布排行（按创建时间降序）
    List<Article> getArticleRankingByLatest();

    // 分页查询文章列表（返回 PageResult，避免序列化 PageImpl）
    // categoryId 为空时查询全部，不为空时按分类筛选
    // keyword 为空时不搜索，不为空时按标题/内容模糊搜索
    PageResult<Article> getAllArticlesPage(int page, int size, String categoryId, String keyword);

    // 文章详情页访问时调用：Redis 阅读量 +1，并标记该文章为脏（待同步到 DB）
    void incrementViewCount(Long articleId);

    // 定时任务：将 Redis 中累计的阅读量增量同步到 MySQL（脏标记 + GETSET 原子归零）
    void syncViewCountToDB();

    void syncLikeCountToDB();
}
