package com.blog.article.service;

import com.blog.article.entity.Comment;

import java.util.List;

public interface CommentService {
    Comment createComment(Long articleId, Long userId, String content, Long parentId);
    List<Comment> getCommentsByArticleId(Long articleId);
    void deleteComment(Long id);
    void deleteCommentsByArticleId(Long articleId);
}
