package com.example.ECM.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartDTO {
    private Long cartId; // Đổi từ id -> cartId cho rõ ràng
    private Long userId;
    private List<CartItemDTO> items; // Đổi tên cho gọn
    private double cartTotal; // Đổi từ totalPrice -> cartTotal
}
