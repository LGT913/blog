package com.blog.blog.service.impl;

import com.blog.blog.entity.Category;
import com.blog.blog.repository.CategoryRepository;
import com.blog.blog.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public Category createCategory(String name, String description){
        // 检查分类名是否已存在
        categoryRepository.findByName(name).ifPresent(c -> { throw new RuntimeException("分类名已存在"); });
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        return categoryRepository.save(category);
    }

    @Override
    public Category getCategoryById(Long id){
        return categoryRepository.findById(id).orElseThrow(() -> new RuntimeException("分类不存在"));
    }

    @Override
    public List<Category> getAllCategories(){
        return categoryRepository.findAll();
    }

    @Override
    public Category updateCategory(Long id, String name, String description){
        Category category=categoryRepository.findById(id).orElseThrow(() -> new RuntimeException("分类不存在"));
        category.setName(name);
        category.setDescription(description);
        return categoryRepository.save(category);
    }

    @Override
    public void deleteCategory(Long id){
        Category category=categoryRepository.findById(id).orElseThrow(() -> new RuntimeException("分类不存在"));
        categoryRepository.delete(category);
    }
}
