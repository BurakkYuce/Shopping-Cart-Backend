package com.datapulse.service;

import com.datapulse.exception.EntityNotFoundException;
import com.datapulse.model.Category;
import com.datapulse.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> getAll() {
        return categoryRepository.findAll();
    }

    public Category getById(String id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category", id));
    }

    public List<Category> getRootCategories() {
        return categoryRepository.findByParentIdIsNull();
    }

    public List<Category> getChildren(String parentId) {
        return categoryRepository.findByParentId(parentId);
    }

    public Category create(Category category) {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        category.setId(id);
        return categoryRepository.save(category);
    }

    public Category update(String id, Category updates) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category", id));

        if (updates.getName() != null) {
            category.setName(updates.getName());
        }
        if (updates.getParentId() != null) {
            category.setParentId(updates.getParentId());
        }

        return categoryRepository.save(category);
    }

    public void delete(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category", id));
        categoryRepository.delete(category);
    }
}
