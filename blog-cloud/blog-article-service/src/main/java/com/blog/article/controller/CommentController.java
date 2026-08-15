package com.blog.article.controller;

import com.blog.article.entity.Comment;
import com.blog.article.service.CommentService;
import com.blog.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/create")
    public Result<Comment> createComment(@RequestParam("articleId") Long articleId,
                                         @RequestParam("userId") Long userId,
                                         @RequestParam("content") String content,
                                         @RequestParam(value = "parentId", required = false) Long parentId) {
        Comment comment = commentService.createComment(articleId, userId, content, parentId);
        return Result.success(comment);
    }

    @GetMapping("/article/{articleId}")
    public Result<List<Comment>> getComment(@PathVariable("articleId") Long articleId) {
        return Result.success(commentService.getCommentsByArticleId(articleId));
    }

    @DeleteMapping("/delete/{id}")
    public Result<String> deleteComment(@PathVariable("id") Long id) {
        commentService.deleteComment(id);
        return Result.success("删除成功");
    }

    @DeleteMapping("/article/{articleId}")
    public Result<String> deleteCommentsByArticleId(@PathVariable("articleId") Long articleId) {
        commentService.deleteCommentsByArticleId(articleId);
        return Result.success("删除成功");
    }
}