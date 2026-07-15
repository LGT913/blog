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
    PageResult<Article> getAllArticlesPage(int page, int size);
}
