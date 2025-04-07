package com.example.ECM.service;

import com.example.ECM.dto.CategoryDTO;
import com.example.ECM.dto.ProductDTO; // Thêm import cho ProductDTO

import java.util.List;
import java.util.Optional;

public interface CategoryService {
    List<CategoryDTO> getAllCategories();
    Optional<CategoryDTO> getCategoryById(Long id);
    CategoryDTO createCategory(CategoryDTO categoryDTO);
    Optional<CategoryDTO> updateCategory(Long id, CategoryDTO updatedCategory);
    boolean deleteCategory(Long id);
    Optional<List<ProductDTO>> getProductsByCategory(Long id);
}