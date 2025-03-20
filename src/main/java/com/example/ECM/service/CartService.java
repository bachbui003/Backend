package com.example.ECM.service;

import com.example.ECM.dto.CartDTO;

import java.util.List;

public interface CartService {
    CartDTO getCartByUserId(Long userId);
    CartDTO addToCart(Long userId, Long productId, int quantity);
    CartDTO updateCartItem(Long userId, Long productId, int quantity);
    void removeCartItem(Long userId, Long productId);
    void clearCart(Long userId);
    List<CartDTO> getAllCarts();

}
