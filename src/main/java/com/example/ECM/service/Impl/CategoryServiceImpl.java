package com.example.ECM.service.Impl;

import com.example.ECM.dto.CategoryDTO;
import com.example.ECM.dto.ProductDTO;
import com.example.ECM.model.Category;
import com.example.ECM.model.Product;
import com.example.ECM.repository.CategoryRepository;
import com.example.ECM.repository.ProductRepository;
import com.example.ECM.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Override
    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<CategoryDTO> getCategoryById(Long id) {
        return categoryRepository.findById(id).map(this::convertToDTO);
    }

    @Override
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        Category category = new Category();
        category.setName(categoryDTO.getName());
        category.setIcon(categoryDTO.getIcon());
        return convertToDTO(categoryRepository.save(category));
    }

    @Override
    public Optional<CategoryDTO> updateCategory(Long id, CategoryDTO updatedCategory) {
        return categoryRepository.findById(id).map(category -> {
            category.setName(updatedCategory.getName());
            category.setIcon(updatedCategory.getIcon());
            return convertToDTO(categoryRepository.save(category));
        });
    }

    @Override
    public boolean deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            return false;
        }
        categoryRepository.deleteById(id);
        return true;
    }

    @Override
    public Optional<List<ProductDTO>> getProductsByCategory(Long id) {
        // Kiểm tra danh mục có tồn tại không
        boolean categoryExists = categoryRepository.existsById(id);
        if (!categoryExists) {
            return Optional.empty(); // Trả về Optional.empty nếu không tồn tại danh mục
        }

        // Lấy danh sách sản phẩm
        List<Product> products = productRepository.findByCategoryId(id);
        List<ProductDTO> productDTOs = products.stream()
                .map(this::convertToProductDTO)
                .collect(Collectors.toList());

        // Trả về cả khi danh sách trống
        return Optional.of(productDTOs);
    }
    private CategoryDTO convertToDTO(Category category) {
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .icon(category.getIcon())
                .build();
    }

    private ProductDTO convertToProductDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .categoryId(product.getCategory().getId())
                .description(product.getDescription())
                .imageUrl(product.getImageUrl())
                .rating(product.getRating())
                .build();
    }

}