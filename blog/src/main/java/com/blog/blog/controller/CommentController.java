package com.blog.blog.controller;

import com.blog.blog.common.result.Result;
import com.blog.blog.dto.CommentNotifyMessage;
import com.blog.blog.entity.Article;
import com.blog.blog.entity.Comment;
import com.blog.blog.service.ArticleService;
import com.blog.blog.service.CommentService;
import com.blog.blog.service.NotifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.blog.blog.common.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@RestController
@RequestMapping("/api/comment")
public class CommentController {
    private static final Logger log = LoggerFactory.getLogger(CommentController.class);

    @Autowired
    private CommentService commentService;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private NotifyService notifyService;

    @PreAuthorize("isAuthenticated()")   // 登录用户即可评论（原来误设为 ADMIN，普通用户无法评论）
    @PostMapping("/create")
    public Result<Comment> createComment(@RequestParam Long articleId,
                                         @AuthenticationPrincipal UserPrincipal principal,
                                         @RequestParam String content,
                                         @RequestParam(required = false) Long parentId) {
        Long userId = principal.getUserId();
        Comment comment = commentService.createComment(articleId, userId, content, parentId);
        // 异步发送通知(不影响主流程,内部已 try-catch)
        try {
            // 查文章作者 ID(需要 ArticleService 注入)
            Article article = articleService.getArticle(articleId);
            CommentNotifyMessage msg = new CommentNotifyMessage();
            msg.setArticleId(articleId);
            msg.setAuthorId(article.getUserId());
            msg.setCommentUserId(userId);
            msg.setCommentContent(content);
            msg.setTimestamp(System.currentTimeMillis());
            notifyService.sendCommentNotify(msg);
        } catch (Exception e) {
            // 查文章失败也要降级,不影响评论主流程
            log.warn("构建评论通知失败,articleId={}", articleId, e);
        }
        return Result.success(comment);
    }

    @GetMapping("/article/{articleId}")
    public Result<List<Comment>> getComment(@PathVariable Long articleId) {
        List<Comment> comments=commentService.getCommentsByArticleId(articleId);
        return Result.success(comments);
    }

    @PreAuthorize("hasRole('ADMIN') or @commentSecurity.isArticleAuthor(authentication, #id)")
    @DeleteMapping("/delete/{id}")
    public Result<String> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return Result.success("删除成功");
    }
    // 删除某篇文章下的所有评论
    @DeleteMapping("/article/{articleId}")
    public Result<String> deleteCommentsByArticleId(@PathVariable Long articleId) {
        commentService.deleteCommentsByArticleId(articleId);
        return Result.success("删除成功");
    }

}
