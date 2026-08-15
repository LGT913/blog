package com.blog.article.repository;

import com.blog.article.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    //查询某篇文章所有评论
    List<Comment> findByArticleIdOrderByCreateTimeDesc(Long articleId);
    //查某条评论的回复
    List<Comment> findByParentId(Long parentId);
    //删除某篇文章的所有评论
    void deleteByArticleId(Long articleId);
}
