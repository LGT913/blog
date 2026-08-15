package com.blog.article.service.impl;

import com.blog.article.entity.Category;
import com.blog.article.repository.CategoryRepository;
import com.blog.article.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

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
