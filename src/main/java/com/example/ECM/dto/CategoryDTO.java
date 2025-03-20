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

    @Builder.Default
    private List<ProductDTO> products = new ArrayList<>();  // Thêm danh sách sản phẩm
}
