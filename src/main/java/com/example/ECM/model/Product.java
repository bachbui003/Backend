package com.example.ECM.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Double price;

    @Column(name = "stock_quantity", nullable = false) // 🔹 Đảm bảo ánh xạ đúng tên cột trong DB
    private Integer stockQuantity = 0; // 🔹 Mặc định là 0 để tránh lỗi null

    @Column(length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private Double rating = 0.0; // 🔹 Mặc định là 0.0 nếu chưa có đánh giá

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
}
