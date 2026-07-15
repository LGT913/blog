package com.blog.blog.controller;

import com.blog.blog.common.Result;
import com.blog.blog.entity.Article;
import com.blog.blog.service.ArticleService;
import com.blog.blog.vo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/article")
public class ArticleController {
    @Autowired
    private ArticleService articleService;

    @PostMapping("/create")
    public Result<Article> create(@RequestBody Article article) {
        Article createdArticle=articleService.createArticle(article.getTitle(),article.getContent(),article.getUserId(),article.getCategoryId());
        return Result.success(createdArticle);
    }

    @GetMapping("/{id}")
    public Result<Article> getArticle(@PathVariable Long id){
        Article article=articleService.getArticle(id);
        return Result.success(article);
    }

    @GetMapping("/list")
    public Result<PageResult<Article>> getAllArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResult<Article> articles = articleService.getAllArticlesPage(page, size);
        return Result.success(articles);
    }

    @GetMapping("/user/{userId}")
    public Result<List<Article>> getUser(@PathVariable Long userId){
        List<Article> articles=articleService.getArticlesByUserId(userId);
        return Result.success(articles);
    }

    @PutMapping("/update/{id}")
    public Result<Article> updateArticle(@PathVariable Long id, @RequestBody Article article) {
        Article updatedArticle = articleService.updateArticle(
            id, article.getTitle(), article.getContent(), article.getCategoryId());
        return Result.success(updatedArticle);
    }

    @DeleteMapping("/delete/{id}")
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
