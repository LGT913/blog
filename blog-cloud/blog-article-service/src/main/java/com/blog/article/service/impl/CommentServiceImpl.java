package com.blog.article.service.impl;

import com.blog.article.config.ArticleConfig;
import com.blog.article.entity.Comment;
import com.blog.article.feign.UserFeignClient;
import com.blog.article.repository.CommentRepository;
import com.blog.article.service.CommentService;
import com.blog.article.util.RedisUtil;
import com.blog.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final RedisUtil redisUtil;
    private final UserFeignClient userFeignClient;
    private final CommentRepository commentRepository;
    private final ArticleConfig articleConfig;

    @Override
    @Transactional
    public Comment createComment(Long articleId, Long userId, String content, Long parentId) {
        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setParentId(parentId);
        comment.setCreateTime(LocalDateTime.now());
        Comment saved = commentRepository.save(comment);
        // 清除缓存
        redisUtil.delete(articleConfig.getPrefix() + ":comment:article:" + articleId);
        return saved;
    }

    @Override
    public List<Comment> getCommentsByArticleId(Long articleId) {
        String cacheKey = articleConfig.getPrefix() + ":comment:article:" + articleId;
        // 先查缓存
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof List) {
            return (List<Comment>) cached;
        }
        // 缓存未命中，查数据库
        List<Comment> comments = commentRepository.findByArticleIdOrderByCreateTimeDesc(articleId);
        // 批量填充昵称
        for (Comment comment : comments) {
            try {
                Result<Map<String, Object>> result = userFeignClient.getUserById(comment.getUserId());
                if (result.getData() != null) {
                    comment.setUsername((String) result.getData().get("username"));
                }
            } catch (Exception e) {
                log.error("Feign 获取用户信息失败, userId={}", comment.getUserId(), e);
                comment.setUsername("用户");
            }
        }
        redisUtil.set(cacheKey, comments, 300); // 缓存5分钟
        return comments;
    }

    @Override
    @Transactional
    public void deleteComment(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("评论不存在"));
        // 删除该评论的所有子回复
        List<Comment> replies = commentRepository.findByParentId(id);
        if (!replies.isEmpty()) {
            commentRepository.deleteAll(replies);
        }
        commentRepository.delete(comment);
    }

    @Override
    @Transactional
    public void deleteCommentsByArticleId(Long articleId) {
        commentRepository.deleteByArticleId(articleId);
    }
}