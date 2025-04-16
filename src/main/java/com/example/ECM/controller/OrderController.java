package com.example.ECM.controller;

import com.example.ECM.dto.MonthlyRevenueDTO;
import com.example.ECM.dto.OrderItemDTO;
import com.example.ECM.dto.OrderResponseDTO;
import com.example.ECM.model.Order;
import com.example.ECM.service.OrderService;
import com.example.ECM.service.CartService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
            return ResponseEntity.badRequest().body(new OrderResponseDTO(null, null, null, null, "FAILED", null, null,null));
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
            return ResponseEntity.status(500).body(null); // Trả về mã lỗi 500 nếu có lỗi máy chủ
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Long id) {
        logger.info("📢 [GET ORDER] Lấy đơn hàng ID: " + id);
        try {
            Order order = orderService.getOrderById(id);
            if (order == null) {
                return ResponseEntity.status(404).body("Không tìm thấy đơn hàng với ID: " + id); // Lỗi 404 nếu không tìm thấy đơn hàng
            }
            logger.info("✅ Đơn hàng tìm thấy: " + order);
            return ResponseEntity.ok(convertToDTO(order));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Không tìm thấy đơn hàng ID: " + id, e);
            return ResponseEntity.status(500).body("Lỗi: " + e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getOrdersByUserId(@PathVariable Long userId) {
        logger.info("📢 [GET USER ORDERS] Lấy đơn hàng của userId: " + userId);
        try {
            List<Order> orders = orderService.getOrdersByUserId(userId);
            if (orders.isEmpty()) {
                return ResponseEntity.status(404).body("Không có đơn hàng cho userId: " + userId);
            }
            logger.info("✅ Số đơn hàng tìm thấy: " + orders.size());
            return ResponseEntity.ok(orders.stream().map(this::convertToDTO).collect(Collectors.toList()));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Lỗi khi lấy đơn hàng của userId: " + userId, e);
            return ResponseEntity.status(500).body("Lỗi: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllOrders() {
        logger.info("📢 [GET ALL ORDERS] Lấy tất cả đơn hàng");
        try {
            List<Order> orders = orderService.getAllOrders();
            if (orders.isEmpty()) {
                return ResponseEntity.status(404).body("Không có đơn hàng nào.");
            }
            logger.info("✅ Tổng số đơn hàng: " + orders.size());
            return ResponseEntity.ok(orders.stream().map(this::convertToDTO).collect(Collectors.toList()));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Lỗi khi lấy tất cả đơn hàng", e);
            return ResponseEntity.status(500).body("Lỗi: " + e.getMessage());
        }
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateOrder(@PathVariable Long id, @RequestBody Order updatedOrder) {
        logger.info("📢 [UPDATE ORDER] Cập nhật đơn hàng ID: " + id + " với dữ liệu mới: " + updatedOrder);
        try {
            Order order = orderService.updateOrder(id, updatedOrder);
            logger.info("✅ Đơn hàng đã cập nhật: " + order);
            return ResponseEntity.ok(convertToDTO(order));
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "❌ Lỗi khi cập nhật đơn hàng ID: " + id, e);
            return ResponseEntity.status(400).body("Lỗi: " + e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Lỗi hệ thống khi cập nhật đơn hàng ID: " + id, e);
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOrder(@PathVariable Long id) {
        try {
            orderService.deleteOrder(id);
            return ResponseEntity.noContent().build(); // Trả về mã HTTP 204 nếu xóa thành công
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi: " + e.getMessage());
        }
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        try {
            Order cancelledOrder = orderService.cancelOrder(orderId);
            return ResponseEntity.ok(cancelledOrder);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body("Lỗi khi hủy đơn hàng: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/revenue/month/{month}")
    public ResponseEntity<MonthlyRevenueDTO> getMonthlyRevenueByMonth(@PathVariable int month) {
        MonthlyRevenueDTO result = orderService.getMonthlyRevenueWithOrdersStats(month);
        return ResponseEntity.ok(result);
    }



    // Lấy sản phẩm bán chạy nhất
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/top-selling-products")
    public ResponseEntity<Map<String, Object>> getTopSellingProducts() {
        Map<String, Object> topSellingProducts = orderService.getTopSellingProducts();
        return ResponseEntity.ok(topSellingProducts);
    }

    // Lấy thống kê số lượng đơn hàng theo trạng thái
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/order-status-stats")
    public ResponseEntity<Map<String, Object>> getOrderStatusStats() {
        Map<String, Object> orderStatusStats = orderService.getOrderStatusStats();
        return ResponseEntity.ok(orderStatusStats);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/revenue")
    public ResponseEntity<MonthlyRevenueDTO> getRevenueBetweenDates(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        try {
            MonthlyRevenueDTO revenueDTO = orderService.getRevenueBetweenDates(startDate, endDate);
            return ResponseEntity.ok(revenueDTO);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Lỗi khi lấy doanh thu từ " + startDate + " đến " + endDate, e);
            return ResponseEntity.status(500).body(null);
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
                order.getUser().getAddress(),
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
