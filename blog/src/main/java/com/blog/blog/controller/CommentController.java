package com.blog.blog.controller;


import com.blog.blog.common.Result;
import com.blog.blog.entity.Comment;
import com.blog.blog.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comment")
public class CommentController {
    @Autowired
    private CommentService commentService;

    @PostMapping("/create")
    public Result<Comment> createComment(@RequestParam Long articleId, @RequestParam Long userId,@RequestParam String content, @RequestParam(required = false) Long parentId) {
        Comment comment =commentService.createComment(articleId, userId, content, parentId);
        return Result.success(comment);
    }

    @GetMapping("/article/{articleId}")
    public Result<List<Comment>> getComment(@PathVariable Long articleId) {
        List<Comment> comments=commentService.getCommentsByArticleId(articleId);
        return Result.success(comments);
    }

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
