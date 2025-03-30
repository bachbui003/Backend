package com.example.ECM.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {
    private Long id;

    @NotBlank(message = "Category name is required")
    @Size(max = 50, message = "Category name should not exceed 50 characters")
    private String name;

    @Size(max = 255, message = "Icon URL should not exceed 255 characters")
    private String icon; // Lưu đường dẫn ảnh hoặc class icon


    @Builder.Default
    private List<ProductDTO> products = new ArrayList<>();  // Thêm danh sách sản phẩm
}
