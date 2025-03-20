package com.example.ECM.controller;

import com.example.ECM.dto.CartDTO;
import com.example.ECM.dto.CartItemRequestDTO;
import com.example.ECM.model.Cart;
import com.example.ECM.model.User;
import com.example.ECM.repository.CartRepository;
import com.example.ECM.repository.UserRepository;
import com.example.ECM.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
@CrossOrigin(origins = "http://localhost:4200") // Cho phép Angular gọi API
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);
    private final CartService cartService;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;

// lấy tất cả giỏ hàng
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/all")
public ResponseEntity<?> getAllCarts() {
    logger.debug("Admin đang lấy danh sách tất cả giỏ hàng");
    try {
        List<CartDTO> carts = cartService.getAllCarts();
        if (carts.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Không tìm thấy giỏ hàng nào"));
        }
        return ResponseEntity.ok(carts);
    } catch (Exception e) {
        logger.error("Lỗi khi lấy danh sách giỏ hàng: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Lỗi khi lấy danh sách giỏ hàng: " + e.getMessage()));
    }
}


    // Lấy giỏ hàng theo userId
    @GetMapping("/{userId}")
    public ResponseEntity<?> getCart(@PathVariable Long userId) {
        logger.debug("Nhận request lấy giỏ hàng của userId: {}", userId);
        try {
            CartDTO cart = cartService.getCartByUserId(userId);
            logger.debug("Giỏ hàng của userId {}: {}", userId, cart);
            return ResponseEntity.ok(cart);
        } catch (Exception e) {
            logger.error("Lỗi khi lấy giỏ hàng của userId {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(500).body("Lỗi khi lấy giỏ hàng: " + e.getMessage());
        }
    }

    // Thêm sản phẩm vào giỏ hàng
    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@Valid @RequestBody CartItemRequestDTO request) {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (!(principal instanceof UserDetails)) {
                return ResponseEntity.status(403).body("Unauthorized");
            }

            UserDetails userDetails = (UserDetails) principal;
            String username = userDetails.getUsername(); // ✅ Lấy username từ token

            // Tìm userId từ database
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Long userId = user.getId(); // ✅ Lấy userId từ database

            CartDTO cartDTO = cartService.addToCart(userId, request.getProductId(), request.getQuantity());
            return ResponseEntity.ok(cartDTO);
        } catch (Exception e) {
            logger.error("Lỗi khi thêm sản phẩm vào giỏ hàng: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Lỗi khi thêm sản phẩm vào giỏ hàng: " + e.getMessage());
        }
    }
    // Cập nhật số lượng sản phẩm trong giỏ hàng
    @PutMapping("/update")
    public ResponseEntity<?> updateCartItem(
            @Valid @RequestBody CartItemRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // Lấy userId từ token
            String username = userDetails.getUsername();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Cập nhật số lượng sản phẩm trong giỏ hàng
            CartDTO updatedCart = cartService.updateCartItem(user.getId(), request.getProductId(), request.getQuantity());
            return ResponseEntity.ok(updatedCart);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @DeleteMapping("/remove")
    public ResponseEntity<?> removeCartItem(
            @Valid @RequestBody CartItemRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // Lấy userId từ token
            String username = userDetails.getUsername();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Gọi service để xóa sản phẩm khỏi giỏ hàng
            cartService.removeCartItem(user.getId(), request.getProductId());
            return ResponseEntity.ok("Sản phẩm đã được xóa khỏi giỏ hàng");

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    // Xóa toàn bộ giỏ hàng của user
    @DeleteMapping("/clear/{userId}")
    public ResponseEntity<?> clearCart(@PathVariable Long userId) {
        try {
            Cart cart = cartRepository.findByUserId(userId)

                    .orElseThrow(() -> new RuntimeException("Cart not found for user ID: " + userId));

            cartRepository.delete(cart); // Xóa hoàn toàn giỏ hàng

            return ResponseEntity.ok(Map.of("message", "Đã xóa toàn bộ giỏ hàng"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Đã xảy ra lỗi khi xóa giỏ hàng"));
        }
    }

}
