package com.example.ECM.service;

import com.example.ECM.dto.ProductDTO;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface ProductService {
    Page<ProductDTO> searchProducts(String name, Long categoryId, Double minPrice, Double maxPrice, Double minRating, int page, int size, String sort);
    Optional<ProductDTO> getProductById(Long id);
    ProductDTO createProduct(ProductDTO productDTO);
    Optional<ProductDTO> updateProduct(Long id, ProductDTO productDTO);
    Optional<ProductDTO> updateStock(Long id, int quantity);
    void deleteProduct(Long id);
    List<ProductDTO> getAllProducts();
}
