package com.example.ECM.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductDTO {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private Integer stockQuantity;  // Có thể là Integer thay vì int để xử lý null nếu cần
    private String imageUrl;
    private Double rating;
    private Long categoryId;

    // Tạo phương thức khởi tạo để đảm bảo giá trị mặc định cho stockQuantity nếu không có
    public ProductDTO(Long id, String name, String description, Double price, Integer stockQuantity, String imageUrl, Double rating, Long categoryId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = (stockQuantity != null) ? stockQuantity : 0;  // Đảm bảo stockQuantity không phải null, mặc định là 0
        this.imageUrl = imageUrl;
        this.rating = rating;
        this.categoryId = categoryId;
    }

    // Tạo builder cho ProductDTO với stockQuantity mặc định là 0 nếu null
    public static class ProductDTOBuilder {
        private Integer stockQuantity = 0;  // Mặc định là 0

        public ProductDTOBuilder stockQuantity(Integer stockQuantity) {
            this.stockQuantity = (stockQuantity != null) ? stockQuantity : 0;  // Đảm bảo stockQuantity không phải null
            return this;
        }
    }
}
