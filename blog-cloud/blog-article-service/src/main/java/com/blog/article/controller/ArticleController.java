package com.blog.article.controller;

import com.blog.article.config.ArticleConfig;
import com.blog.article.entity.Article;
import com.blog.article.service.ArticleService;
import com.blog.common.result.Result;
import com.blog.common.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/article")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;
    private final ArticleConfig articleConfig;

    @PostMapping("/create")
    public Result<Article> create(@RequestBody Article article) {
        return Result.success(articleService.createArticle(
                article.getTitle(), article.getContent(),
                article.getUserId(), article.getCategoryId()));
    }

    @GetMapping("/{id}")
    public Result<Article> getById(@PathVariable("id") Long id) {
        Article article = articleService.getArticle(id);
        if (article == null) {
            return Result.error("文章不存在");
        }
        articleService.incrementViewCount(id);
        return Result.success(article);
    }

    @GetMapping("/list")
    public Result<List<Article>> list() {
        return Result.success(articleService.getAllArticles());
    }

    @GetMapping("/user/{userId}")
    public Result<List<Article>> getByUserId(@PathVariable("userId") Long userId) {
        if (userId == null) {
            return Result.error("用户ID不能为空");
        }
        return Result.success(articleService.getArticlesByUserId(userId));
    }

    @GetMapping("/page")
    public Result<PageResult<Article>> page(@RequestParam(name = "page", defaultValue = "0") int page,
                                            @RequestParam(name = "size", defaultValue = "10") int size,
                                            @RequestParam(name = "categoryId", required = false) String categoryId,
                                            @RequestParam(name = "keyword", required = false) String keyword) {
        return Result.success(articleService.getAllArticlesPage(page, size, categoryId, keyword));
    }

    @PutMapping("/{id}")
    public Result<Article> update(@PathVariable Long id, @RequestBody Article article) {
        return Result.success(articleService.updateArticle(
                id, article.getTitle(), article.getContent(), article.getCategoryId()));
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return Result.success("删除成功");
    }

    @GetMapping("/ranking/views")
    public Result<List<Article>> rankingByViews() {
        return Result.success(articleService.getArticleRankingByViews());
    }

    @GetMapping("/ranking/latest")
    public Result<List<Article>> rankingByLatest() {
        return Result.success(articleService.getArticleRankingByLatest());
    }

    @GetMapping("/config")
    public Result<Map<String, Object>> config() {
        return Result.success(Map.of("prefix", articleConfig.getPrefix()));
    }
}