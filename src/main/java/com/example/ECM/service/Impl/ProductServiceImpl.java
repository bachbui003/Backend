package com.example.ECM.service.Impl;

import com.example.ECM.dto.ProductDTO;
import com.example.ECM.model.Category;
import com.example.ECM.model.Product;
import com.example.ECM.repository.CategoryRepository;
import com.example.ECM.repository.ProductRepository;
import com.example.ECM.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public Page<ProductDTO> searchProducts(String name, Long categoryId, Double minPrice, Double maxPrice, Double minRating, int page, int size, String sort) {
        Sort sorting = switch (sort) {
            case "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            case "rating_desc" -> Sort.by("rating").descending();
            default -> Sort.unsorted();
        };

        Pageable pageable = PageRequest.of(page, size, sorting);
        return productRepository.searchProducts(name, categoryId, minPrice, maxPrice, minRating, pageable)
                .map(this::convertToDTO);
    }

    @Override
    public Optional<ProductDTO> getProductById(Long id) {
        return productRepository.findById(id).map(this::convertToDTO);
    }

    @Override
    public ProductDTO createProduct(ProductDTO productDTO) {
        Category category = categoryRepository.findById(productDTO.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));

        Product product = new Product();
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setStockQuantity(productDTO.getStockQuantity());
        product.setImageUrl(productDTO.getImageUrl());
        product.setRating(productDTO.getRating());
        product.setCategory(category);

        return convertToDTO(productRepository.save(product));
    }

    @Override
    public Optional<ProductDTO> updateProduct(Long id, ProductDTO productDTO) {
        return productRepository.findById(id).map(existingProduct -> {
            existingProduct.setStockQuantity(productDTO.getStockQuantity() != null ? productDTO.getStockQuantity() : 0);
            existingProduct.setName(productDTO.getName());
            existingProduct.setDescription(productDTO.getDescription());
            existingProduct.setPrice(productDTO.getPrice());
            existingProduct.setImageUrl(productDTO.getImageUrl());
            existingProduct.setRating(productDTO.getRating());

            if (productDTO.getCategoryId() != null) {
                Category category = categoryRepository.findById(productDTO.getCategoryId())
                        .orElseThrow(() -> new RuntimeException("Category not found"));
                existingProduct.setCategory(category);
            }

            return convertToDTO(productRepository.save(existingProduct));
        });
    }

    @Override
    public Optional<ProductDTO> updateStock(Long id, int quantity) {
        return productRepository.findById(id).map(existingProduct -> {
            existingProduct.setStockQuantity(quantity);
            productRepository.save(existingProduct);
            return convertToDTO(existingProduct);
        });
    }

    @Override
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    @Override
    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private ProductDTO convertToDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .imageUrl(product.getImageUrl())
                .rating(product.getRating())
                .categoryId(product.getCategory().getId())
                .build();
    }
}
