package com.blog.blog.service;

import com.blog.blog.entity.Article;

import java.util.List;

public interface ArticleService {
    Article createArticle(String title, String content, Long userId,String categoryId);
    Article getArticle(Long id);
    List<Article> getAllArticles();
    List<Article> getArticlesByUserId(Long userId);
    Article updateArticle(Long id, String title, String content);
    void deleteArticle(Long id);
}
