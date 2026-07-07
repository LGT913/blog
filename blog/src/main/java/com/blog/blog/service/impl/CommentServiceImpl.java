package com.blog.blog.service.impl;

import com.blog.blog.entity.Comment;
import com.blog.blog.repository.CommentRepository;
import com.blog.blog.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommentServiceImpl implements CommentService{
    @Autowired
    private CommentRepository commentRepository;

    @Override
    public Comment createComment(Long articleId, Long userId, String content, Long parentId){
        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setParentId(parentId);
        comment.setCreateTime(LocalDateTime.now());
        return commentRepository.save(comment);
    }

    @Override
    public List<Comment> getCommentsByArticleId(Long articleId){
        return commentRepository.findByArticleIdOrderByCreateTimeDesc(articleId);
    }

    @Override
    public void deleteComment(Long id){
        Comment comment=commentRepository.findById(id).orElseThrow(() -> new RuntimeException("评论不存在"));
        List<Comment> replies=commentRepository.findByParentId(comment.getParentId());
        if(!replies.isEmpty()){
            commentRepository.deleteAll(replies);
        }
        commentRepository.delete(comment);
    }

    @Override
    public void deleteCommentsByArticleId(Long articleId){
        commentRepository.deleteByArticleId(articleId);
    }
}
