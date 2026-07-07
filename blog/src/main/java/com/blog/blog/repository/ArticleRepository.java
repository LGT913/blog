package com.blog.blog.repository;


import com.blog.blog.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    List<Article> findByUserIdOrderByCreateTimeDesc(Long userId);
    List<Article> findAllByOrderByCreateTimeDesc();
}
