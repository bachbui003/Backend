package com.example.ECM.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemDTO {
    private Long orderItemId;
    private Long productId;  // Chỉ lưu ID sản phẩm
    private String productName;
    private String productDescription;
    private BigDecimal productPrice;
    private String productImageUrl;
    private int quantity;

}