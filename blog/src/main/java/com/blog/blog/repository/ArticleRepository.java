package com.blog.blog.repository;


import com.blog.blog.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    List<Article> findByUserIdOrderByCreateTimeDesc(Long userId);
    List<Article> findAllByOrderByCreateTimeDesc();

    // 获取阅读量排行（前10条，按阅读数降序排列）
    List<Article> findTop10ByOrderByViewCountDesc();

    // 获取最新发布排行（前10条，按创建时间降序排列）
    List<Article> findTop10ByOrderByCreateTimeDesc();
}
