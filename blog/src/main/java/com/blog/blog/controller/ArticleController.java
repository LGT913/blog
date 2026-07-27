package com.blog.blog.controller;

import com.blog.blog.common.Result;
import com.blog.blog.entity.Article;
import com.blog.blog.service.ArticleService;
import com.blog.blog.vo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.blog.blog.common.RedisUtil;
import com.blog.blog.common.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/article")
public class ArticleController {
    private final ArticleService articleService;
    private final RedisUtil redisUtil;

    public ArticleController(ArticleService articleService, RedisUtil redisUtil) {
        this.articleService = articleService;
        this.redisUtil = redisUtil;
    }

    @PostMapping("/create")
    public Result<Article> create(@RequestBody Article article,
                                  @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal.getUserId();   // ← 从 JWT 取，不可伪造

        // 锁 key：同一用户同一时间只能创建一篇文章
        String lockKey = "article:create:lock:" + userId;
        String lockValue = UUID.randomUUID().toString();

        boolean locked = redisUtil.tryLock(lockKey, lockValue, 30);
        if (!locked) {
            return Result.error("操作太频繁，请稍后重试");
        }
        try {
            Article createdArticle = articleService.createArticle(
                    article.getTitle(), article.getContent(),
                    userId, article.getCategoryId());
            return Result.success(createdArticle);
        } finally {
            redisUtil.unlock(lockKey, lockValue);
        }
    }

    @GetMapping("/{id}")
    public Result<Article> getArticle(@PathVariable Long id){
        Article article=articleService.getArticle(id);
        return Result.success(article);
    }

    @GetMapping("/list")
    public Result<PageResult<Article>> getAllArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String keyword) {
        PageResult<Article> articles = articleService.getAllArticlesPage(page, size, categoryId, keyword);
        return Result.success(articles);
    }

    @GetMapping("/user/{userId}")
    public Result<List<Article>> getUser(@PathVariable Long userId){
        List<Article> articles=articleService.getArticlesByUserId(userId);
        return Result.success(articles);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")   // 只有管理员能改任何文章
    public Result<Article> updateArticle(@PathVariable Long id, @RequestBody Article article) {
        Article updatedArticle = articleService.updateArticle(
            id, article.getTitle(), article.getContent(), article.getCategoryId());
        return Result.success(updatedArticle);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> deleteArticle(@PathVariable Long id){
        articleService.deleteArticle(id);
        return Result.success("删除成功");
    }

    @GetMapping("/ranking/views")
    public Result<List<Article>> getArticleRankingByViews() {
        List<Article> articles = articleService.getArticleRankingByViews();
        return Result.success(articles);
    }

    @GetMapping("/ranking/latest")
    public Result<List<Article>> getArticleRankingByLatest() {
        List<Article> articles = articleService.getArticleRankingByLatest();
        return Result.success(articles);
    }
}
