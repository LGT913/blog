package com.blog.blog.service;

import com.blog.blog.entity.Comment;

import java.util.List;

public interface CommentService {
    Comment createComment(Long articleId, Long userId, String content, Long parentId);
    List<Comment> getCommentsByArticleId(Long articleId);
    void deleteComment(Long id);
    void deleteCommentsByArticleId(Long articleId);
}
