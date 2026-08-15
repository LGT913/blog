package com.blog.blog.service.impl;

import com.blog.blog.common.util.RedisUtil;
import com.blog.blog.config.RabbitMQConfig;
import com.blog.blog.dto.CommentNotifyMessage;
import com.blog.blog.entity.Article;
import com.blog.blog.entity.Comment;
import com.blog.blog.repository.ArticleRepository;
import com.blog.blog.repository.CommentRepository;
import com.blog.blog.repository.UserRepository;
import com.blog.blog.service.CommentService;
import com.blog.blog.entity.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentServiceImpl.class);
    private static final String COMMENT_CACHE_KEY = "blog:comment:article:";
    private static final long COMMENT_CACHE_EXPIRE = 600L;

    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final RedisUtil redisUtil;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public Comment createComment(Long articleId, Long userId, String content, Long parentId) {
        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setParentId(parentId);
        comment.setCreateTime(LocalDateTime.now());
        comment = commentRepository.save(comment);

        // 填充评论者昵称（返回给前端展示，避免显示兜底"用户"）
        // 用 final 引用：lambda 只能捕获 effectively final 的变量
        final Comment savedComment = comment;
        userRepository.findById(userId).ifPresent(u -> savedComment.setUsername(u.getNickname()));

        // 清除该文章的评论缓存
        redisUtil.delete(COMMENT_CACHE_KEY + articleId);

        // 异步通知文章作者
        try {
            Article article = articleRepository.findById(articleId).orElse(null);
            if (article != null && !article.getUserId().equals(userId)) {
                CommentNotifyMessage msg = new CommentNotifyMessage();
                msg.setArticleId(articleId);
                msg.setAuthorId(article.getUserId());
                msg.setCommentUserId(userId);
                msg.setCommentContent(content);
                msg.setTimestamp(System.currentTimeMillis());

                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.COMMENT_EXCHANGE,
                        RabbitMQConfig.COMMENT_ROUTING_KEY,
                        msg
                );
            }
        } catch (Exception e) {
            log.warn("发送评论通知消息失败, articleId={}, userId={}", articleId, userId, e);
        }

        return comment;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Comment> getCommentsByArticleId(Long articleId) {
        String key = COMMENT_CACHE_KEY + articleId;
        Object cacheObj = redisUtil.get(key);
        List<Comment> comments;

        if (cacheObj instanceof List) {
            comments = (List<Comment>) cacheObj;
            // 旧缓存中的评论可能缺 username（实体新增字段前写入的），需要补填
            if (comments.isEmpty() || comments.get(0).getUsername() != null) {
                return comments;
            }
            fillUsernames(comments);
            redisUtil.set(key, comments, COMMENT_CACHE_EXPIRE);  // 补填后回写缓存
            return comments;
        }

        comments = commentRepository.findByArticleIdOrderByCreateTimeDesc(articleId);
        if (comments.isEmpty()) {
            redisUtil.set(key, Collections.emptyList(), 60L);
            return comments;
        }
        fillUsernames(comments);
        redisUtil.set(key, comments, COMMENT_CACHE_EXPIRE);
        return comments;
    }

    /**
     * 批量填充评论者昵称（一次查 user 表，避免 N+1 查询）
     */
    private void fillUsernames(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) return;
        Set<Long> userIds = comments.stream()
                .map(Comment::getUserId)
                .collect(Collectors.toSet());
        Map<Long, String> nicknameMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickname, (a, b) -> a));
        comments.forEach(c -> c.setUsername(nicknameMap.getOrDefault(c.getUserId(), "用户")));
    }

    @Override
    @Transactional
    public void deleteComment(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("评论不存在"));

        // 先删除该评论的所有子回复
        List<Comment> replies = commentRepository.findByParentId(id);
        if (!replies.isEmpty()) {
            commentRepository.deleteAll(replies);
        }

        // 再删除评论本身
        commentRepository.delete(comment);

        // 清除缓存
        redisUtil.delete(COMMENT_CACHE_KEY + comment.getArticleId());
    }

    @Override
    @Transactional
    public void deleteCommentsByArticleId(Long articleId) {
        commentRepository.deleteByArticleId(articleId);
        redisUtil.delete(COMMENT_CACHE_KEY + articleId);
    }
}