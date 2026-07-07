package com.blog.blog.service;

import com.blog.blog.entity.Category;

import java.util.List;

public interface CategoryService {
    Category createCategory(String name, String description);
    Category getCategoryById(Long id);
    List<Category> getAllCategories();
    Category updateCategory(Long id,String name, String description);
    void deleteCategory(Long id);
}
