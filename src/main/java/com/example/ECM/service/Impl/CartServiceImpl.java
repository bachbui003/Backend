package com.example.ECM.service.Impl;

import com.example.ECM.dto.CartDTO;
import com.example.ECM.dto.CartItemDTO;
import com.example.ECM.dto.ProductDTO;
import com.example.ECM.model.Cart;
import com.example.ECM.model.CartItem;
import com.example.ECM.model.Product;
import com.example.ECM.model.User;
import com.example.ECM.repository.CartItemRepository;
import com.example.ECM.repository.CartRepository;
import com.example.ECM.repository.ProductRepository;
import com.example.ECM.repository.UserRepository;
import com.example.ECM.service.CartService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartServiceImpl.class);

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    public CartDTO getCartByUserId(Long userId) {
        logger.debug("Lấy giỏ hàng cho userId: {}", userId);
        try {
            Cart cart = cartRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng cho user ID: " + userId));
            return convertToDTO(cart);
        } catch (Exception e) {
            logger.error("Lỗi khi lấy giỏ hàng của user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Lỗi khi lấy giỏ hàng: " + e.getMessage());
        }
    }

    @Override
    public List<CartDTO> getAllCarts() {
        logger.debug("Lấy danh sách tất cả giỏ hàng");
        try {
            List<Cart> carts = cartRepository.findAll();
            return carts.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Lỗi khi lấy danh sách giỏ hàng: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi lấy danh sách giỏ hàng: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public CartDTO addToCart(Long userId, Long productId, int quantity) {
        logger.debug("Thêm sản phẩm vào giỏ hàng - userId: {}, productId: {}, quantity: {}", userId, productId, quantity);
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

            Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> {
                Cart newCart = new Cart();
                newCart.setUser(user);
                return cartRepository.save(newCart);
            });

            Optional<CartItem> existingItem = cartItemRepository.findByCartAndProduct(cart, product);
            if (existingItem.isPresent()) {
                CartItem cartItem = existingItem.get();
                cartItem.updateQuantity(cartItem.getQuantity() + quantity);
            } else {
                CartItem newItem = new CartItem(cart, product, quantity);
                cart.addItem(newItem);
                cartItemRepository.save(newItem);
            }

            return convertToDTO(cart);
        } catch (Exception e) {
            logger.error("Lỗi khi thêm sản phẩm vào giỏ hàng: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi thêm sản phẩm vào giỏ hàng: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public CartDTO updateCartItem(Long userId, Long productId, int quantity) {
        logger.debug("Cập nhật sản phẩm trong giỏ hàng - userId: {}, productId: {}, quantity: {}", userId, productId, quantity);
        try {
            Cart cart = cartRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng cho user ID: " + userId));

            CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, productRepository.getReferenceById(productId))
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ hàng"));

            cartItem.updateQuantity(quantity);
            cartItemRepository.save(cartItem);
            return convertToDTO(cart);
        } catch (Exception e) {
            logger.error("Lỗi khi cập nhật sản phẩm trong giỏ hàng: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi cập nhật sản phẩm trong giỏ hàng: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void removeCartItem(Long userId, Long productId) {
        logger.debug("Xóa sản phẩm khỏi giỏ hàng - userId: {}, productId: {}", userId, productId);
        try {
            Cart cart = cartRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng cho user ID: " + userId));

            CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, productRepository.getReferenceById(productId))
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ hàng"));

            cart.removeItem(cartItem);
            cartItemRepository.delete(cartItem);
        } catch (Exception e) {
            logger.error("Lỗi khi xóa sản phẩm khỏi giỏ hàng: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi xóa sản phẩm khỏi giỏ hàng: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        logger.debug("Xóa toàn bộ giỏ hàng của userId: {}", userId);
        try {
            Cart cart = cartRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng cho user ID: " + userId));

            cart.clearItems();
            cartRepository.save(cart);
        } catch (Exception e) {
            logger.error("Lỗi khi xóa toàn bộ giỏ hàng: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi xóa toàn bộ giỏ hàng: " + e.getMessage());
        }
    }

    private CartDTO convertToDTO(Cart cart) {
        List<CartItemDTO> cartItemDTOs = cart.getCartItems().stream()
                .map(item -> new CartItemDTO(
                        item.getId(),
                        new ProductDTO(
                                item.getProduct().getId(),
                                item.getProduct().getName(),
                                item.getProduct().getDescription(),
                                item.getProduct().getPrice(),
                                item.getProduct().getStockQuantity(),
                                item.getProduct().getImageUrl(),
                                item.getProduct().getRating(),
                                item.getProduct().getCategory().getId()
                        ),
                        item.getQuantity(),
                        item.getQuantity() * item.getProduct().getPrice() // ✅ Tính tổng tiền cho từng sản phẩm
                ))
                .collect(Collectors.toList());

        double totalPrice = cartItemDTOs.stream()
                .mapToDouble(CartItemDTO::getPrice)
                .sum();

        return new CartDTO(
                cart.getId(),
                cart.getUser().getId(),
                cartItemDTOs,
                totalPrice
        );
    }

}
