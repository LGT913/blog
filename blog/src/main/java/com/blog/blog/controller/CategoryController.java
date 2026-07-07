package com.blog.blog.controller;


import com.blog.blog.common.Result;
import com.blog.blog.entity.Category;
import com.blog.blog.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/category")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    @PostMapping("/create")
    public Result<Category> create(@RequestBody Category category) {
        Category createCategory=categoryService.createCategory(category.getName(), category.getDescription());
        return Result.success(createCategory);
    }

    @GetMapping("/{id}")
    public Result<Category> getCategory(@PathVariable Long id) {
        Category category=categoryService.getCategoryById(id);
        return Result.success(category);
    }

    @GetMapping("/list")
    public Result<List<Category>> getAllCategories(){
        List<Category> categoryList=categoryService.getAllCategories();
        return Result.success(categoryList);
    }

    @PutMapping("/update/{id}")
    public Result<Category> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        Category updateCategory=categoryService.updateCategory(id, category.getName(), category.getDescription());
        return Result.success(updateCategory);
    }

    @DeleteMapping("/delete/{id}")
    public Result<String> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success("删除成功");
    }
}
