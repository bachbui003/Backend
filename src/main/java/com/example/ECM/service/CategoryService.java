package com.example.ECM.service;

import com.example.ECM.dto.CategoryDTO;

import java.util.List;
import java.util.Optional;

public interface CategoryService {
    List<CategoryDTO> getAllCategories();
    Optional<CategoryDTO> getCategoryById(Long id);
    CategoryDTO createCategory(CategoryDTO categoryDTO);
    Optional<CategoryDTO> updateCategory(Long id, CategoryDTO updatedCategory);
    boolean deleteCategory(Long id);
}
