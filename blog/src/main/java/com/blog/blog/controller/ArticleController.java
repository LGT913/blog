package com.blog.blog.controller;

import com.blog.blog.common.Result;
import com.blog.blog.entity.Article;
import com.blog.blog.service.ArticleService;
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
    public Result<List<Article>> getAllArticles(){
        List<Article> articles=articleService.getAllArticles();
        return Result.success(articles);
    }

    @GetMapping("/user/{userId}")
    public Result<List<Article>> getUser(@PathVariable Long userId){
        List<Article> articles=articleService.getArticlesByUserId(userId);
        return Result.success(articles);
    }

    @PutMapping("/update/{id}")
    public Result<Article> updateArticle(@PathVariable Long id, @RequestParam String title, @RequestParam String content) {
        Article article=articleService.updateArticle(id,title,content);
        return Result.success(article);
    }

    @DeleteMapping("/delete/{id}")
    public Result<String> deleteArticle(@PathVariable Long id){
        articleService.deleteArticle(id);
        return Result.success("删除成功");
    }
}
