package com.example.ECM.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartItemRequestDTO {
    private Long userId;
    private Long productId;
    private int quantity;
}
