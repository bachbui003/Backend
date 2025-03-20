package com.example.ECM.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {
    private Long cartItemId;
    private ProductDTO product;
    private int quantity;
    private double price; // ✅ Tổng giá của sản phẩm đó trong giỏ hàng (quantity * product price)
}
