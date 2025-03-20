package com.example.ECM.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "cart_items")
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private int quantity;

    private double price; // Nếu không có, hãy thêm vào!

    public CartItem(Cart cart, Product product, int quantity) {
        this.cart = cart;
        this.product = product;
        this.quantity = quantity;
        this.price = product.getPrice(); // Gán giá khi tạo item mới
    }

    public void updateQuantity(int quantity) {
        this.quantity = quantity;
        this.price = this.product.getPrice() * quantity; // Cập nhật giá khi thay đổi số lượng
    }
}
