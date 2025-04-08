package com.example.ECM.controller;

import com.example.ECM.dto.OrderItemDTO;
import com.example.ECM.dto.OrderResponseDTO;
import com.example.ECM.model.Order;
import com.example.ECM.service.OrderService;
import com.example.ECM.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger logger = Logger.getLogger(OrderController.class.getName());

    private final OrderService orderService;
    private final CartService cartService;

    public OrderController(OrderService orderService, CartService cartService) {
        this.orderService = orderService;
        this.cartService = cartService;
    }

    @PostMapping("/checkout/{userId}")
    public ResponseEntity<OrderResponseDTO> checkout(
            @PathVariable Long userId,
            @RequestBody List<Long> selectedCartItemIds) {
        logger.info("📢 [CHECKOUT] Tạo đơn hàng cho userId: " + userId + " với các cartItemIds: " + selectedCartItemIds);
        try {
            Order newOrder = orderService.createOrder(userId, selectedCartItemIds);
            cartService.removeSelectedItems(userId, selectedCartItemIds);
            logger.info("✅ Đơn hàng đã tạo và các sản phẩm đã được xóa khỏi giỏ hàng: " + newOrder);
            return ResponseEntity.ok(convertToDTO(newOrder));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Lỗi khi tạo đơn hàng hoặc xóa giỏ hàng cho userId: " + userId, e);
            return ResponseEntity.badRequest().body(new OrderResponseDTO(null, null, null, null, "FAILED", null, null));
        }
    }

    @PostMapping("/create")
    public ResponseEntity<Order> createOrder(
            @RequestParam Long userId,
            @RequestBody List<Long> selectedCartItemIds) {
        logger.info("📢 [CREATE ORDER] Tạo đơn hàng cho userId: " + userId + " với các cartItemIds: " + selectedCartItemIds);
        try {
            Order order = orderService.createOrder(userId, selectedCartItemIds);
            cartService.removeSelectedItems(userId, selectedCartItemIds);
            logger.info("✅ Đơn hàng đã tạo: " + order);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Lỗi khi tạo đơn hàng cho userId: " + userId, e);
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Long id) {
        logger.info("📢 [GET ORDER] Lấy đơn hàng ID: " + id);
        try {
            Order order = orderService.getOrderById(id);
            logger.info("✅ Đơn hàng tìm thấy: " + order);
            return ResponseEntity.ok(convertToDTO(order));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Không tìm thấy đơn hàng ID: " + id, e);
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getOrdersByUserId(@PathVariable Long userId) {
        logger.info("📢 [GET USER ORDERS] Lấy đơn hàng của userId: " + userId);
        try {
            List<Order> orders = orderService.getOrdersByUserId(userId);
            logger.info("✅ Số đơn hàng tìm thấy: " + orders.size());
            return ResponseEntity.ok(orders.stream().map(this::convertToDTO).collect(Collectors.toList()));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Lỗi khi lấy đơn hàng của userId: " + userId, e);
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllOrders() {
        logger.info("📢 [GET ALL ORDERS] Lấy tất cả đơn hàng");
        try {
            List<Order> orders = orderService.getAllOrders();
            logger.info("✅ Tổng số đơn hàng: " + orders.size());
            return ResponseEntity.ok(orders.stream().map(this::convertToDTO).collect(Collectors.toList()));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Lỗi khi lấy tất cả đơn hàng", e);
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateOrder(@PathVariable Long id, @RequestBody Order updatedOrder) {
        logger.info("📢 [UPDATE ORDER] Cập nhật đơn hàng ID: " + id + " với trạng thái mới: " + updatedOrder.getStatus());
        try {
            Order order = orderService.updateOrder(id, updatedOrder);
            logger.info("✅ Đơn hàng đã cập nhật: " + order);
            return ResponseEntity.ok(convertToDTO(order));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Lỗi khi cập nhật đơn hàng ID: " + id, e);
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrder(@PathVariable Long id) {
        logger.info("📢 [DELETE ORDER] Xóa đơn hàng ID: " + id);
        try {
            orderService.deleteOrder(id);
            logger.info("✅ Đã xóa đơn hàng ID: " + id);
            return ResponseEntity.ok("Order deleted successfully");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Lỗi khi xóa đơn hàng ID: " + id, e);
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }
    // 🚫 API hủy đơn hàng
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        try {
            Order cancelledOrder = orderService.cancelOrder(orderId);
            return ResponseEntity.ok(cancelledOrder);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private OrderResponseDTO convertToDTO(Order order) {
        if (order == null) {
            throw new RuntimeException("Đơn hàng không tồn tại.");
        }

        if (order.getUser() == null) {
            throw new RuntimeException("Người dùng của đơn hàng không tồn tại.");
        }

        return new OrderResponseDTO(
                order.getId(),
                order.getUser().getUsername(),
                order.getUser().getEmail(),
                order.getTotalPrice(),
                order.getStatus(),
                order.getOrderDate() != null ? order.getOrderDate().toString() : null,
                order.getOrderItems().stream()
                        .map(item -> {
                            if (item.getProduct() == null) {
                                throw new RuntimeException("Sản phẩm trong đơn hàng bị lỗi.");
                            }
                            return new OrderItemDTO(
                                    item.getId(),
                                    item.getProduct().getId(),
                                    item.getProduct().getName(),
                                    item.getProduct().getDescription(),
                                    BigDecimal.valueOf(item.getProduct().getPrice()),
                                    item.getProduct().getImageUrl(),
                                    item.getQuantity()
                            );
                        })
                        .collect(Collectors.toList())
        );
    }
}