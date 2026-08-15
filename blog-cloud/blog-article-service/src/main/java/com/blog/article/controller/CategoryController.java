package com.blog.article.controller;

import com.blog.article.entity.Category;
import com.blog.article.service.CategoryService;
import com.blog.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping("/create")
    public Result<Category> create(@RequestBody Category category) {
        Category created = categoryService.createCategory(category.getName(), category.getDescription());
        return Result.success(created);
    }

    @GetMapping("/{id}")
    public Result<Category> getCategory(@PathVariable("id") Long id) {
        return Result.success(categoryService.getCategoryById(id));
    }

    @GetMapping("/list")
    public Result<List<Category>> getAllCategories() {
        return Result.success(categoryService.getAllCategories());
    }

    @PutMapping("/update/{id}")
    public Result<Category> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        return Result.success(categoryService.updateCategory(id, category.getName(), category.getDescription()));
    }

    @DeleteMapping("/delete/{id}")
    public Result<String> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success("删除成功");
    }
}