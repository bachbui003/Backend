package com.example.ECM.repository;

import com.example.ECM.model.Cart;
import com.example.ECM.model.CartItem;
import com.example.ECM.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);
}
