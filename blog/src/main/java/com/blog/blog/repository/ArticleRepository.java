package com.blog.blog.repository;

import com.blog.blog.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    List<Article> findByUserIdOrderByCreateTimeDesc(Long userId);
    List<Article> findAllByOrderByCreateTimeDesc();

    // 获取阅读量排行（前10条，按阅读数降序排列）
    List<Article> findTop10ByOrderByViewCountDesc();

    // 获取最新发布排行（前10条，按创建时间降序排列）
    List<Article> findTop10ByOrderByCreateTimeDesc();

    // 分页查询，按创建时间倒序
    Page<Article> findAllByOrderByCreateTimeDesc(Pageable pageable);

    // 按分类分页查询，按创建时间倒序
    Page<Article> findByCategoryIdOrderByCreateTimeDesc(String categoryId, Pageable pageable);

    // 按关键词搜索分页（标题或内容包含），按创建时间倒序
    Page<Article> findByTitleContainingOrContentContainingOrderByCreateTimeDesc(
            String title, String content, Pageable pageable);

    // 按分类+关键词搜索分页，按创建时间倒序
    @Query("SELECT a FROM Article a WHERE a.categoryId = :cid AND (a.title LIKE :kw OR a.content LIKE :kw)")
    Page<Article> searchByCategoryAndKeyword(@Param("cid") String categoryId,
                                             @Param("kw") String keyword,
                                             Pageable pageable);
    // 查询所有文章 ID（给布隆过滤器初始化用）
    @Query("SELECT a.id FROM Article a")
    List<Long> findAllIds();

    // 阅读量同步：按增量更新（而非覆盖），原子操作
    @Modifying
    @Transactional
    @Query("UPDATE Article a SET a.viewCount = a.viewCount + :delta WHERE a.id = :id")
    int incrementViewCount(@Param("id") Long id, @Param("delta") int delta);

    //点赞
    @Modifying
    @Transactional
    @Query("UPDATE Article a SET a.likeCount = a.likeCount + :delta WHERE a.id = :id")
    int incrementLikeCount(@Param("id") Long id, @Param("delta") int delta);

    @Modifying
    @Transactional
    @Query("UPDATE Article a SET a.likeCount = :count WHERE a.id = :id")
    int updateLikeCount(@Param("id") Long id, @Param("count") int count);
}
