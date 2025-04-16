package com.example.ECM.service.Impl;

import com.example.ECM.dto.MonthlyRevenueDTO;
import com.example.ECM.dto.TopProductDTO;
import com.example.ECM.model.*;
import com.example.ECM.repository.*;
import com.example.ECM.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = Logger.getLogger(OrderServiceImpl.class.getName());

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository, CartRepository cartRepository,
                            CartItemRepository cartItemRepository, OrderItemRepository orderItemRepository,
                            ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
    }

    // Các phương thức CRUD đơn hàng

    @Override
    public Order createOrder(Long userId, List<Long> selectedCartItemIds) {
        logger.info("Bắt đầu tạo đơn hàng cho userId: " + userId);

        // Lấy giỏ hàng của user
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng"));

        // Lọc ra danh sách sản phẩm được chọn
        List<CartItem> selectedCartItems = cart.getCartItems().stream()
                .filter(cartItem -> selectedCartItemIds.contains(cartItem.getId()))
                .toList();

        if (selectedCartItems.isEmpty()) {
            throw new RuntimeException("Chưa chọn sản phẩm nào để đặt hàng!");
        }

        // Tạo đơn hàng mới
        Order newOrder = new Order();
        newOrder.setUser(cart.getUser());
        newOrder.setOrderDate(LocalDateTime.now());
        newOrder.setStatus(OrderStatus.PENDING.name());

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (CartItem cartItem : selectedCartItems) {
            Product product = cartItem.getProduct();
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException("Sản phẩm không đủ hàng!");
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(newOrder);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(BigDecimal.valueOf(product.getPrice()).multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            orderItem.setImageUrl(product.getImageUrl());
            orderItems.add(orderItem);
            totalPrice = totalPrice.add(orderItem.getPrice());

            // Giảm số lượng tồn kho
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);
        }

        newOrder.setTotalPrice(totalPrice);
        newOrder.setOrderItems(orderItems);
        Order savedOrder = orderRepository.save(newOrder);

        // Xóa các sản phẩm đã chọn khỏi giỏ hàng
        cartItemRepository.deleteAll(selectedCartItems);
        logger.info("Đã xóa sản phẩm đã chọn trong giỏ hàng sau khi đặt hàng.");

        // Nếu giỏ hàng trống thì xóa luôn
        if (cart.getCartItems().isEmpty()) {
            cartRepository.delete(cart);
            logger.info("Đã xóa giỏ hàng vì không còn sản phẩm.");
        }

        return savedOrder;
    }

    @Override
    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
    }

    @Override
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Override
    public Order updateOrder(Long id, Order updatedOrder) {
        return orderRepository.findById(id).map(order -> {
            // Update status if provided
            if (updatedOrder.getStatus() != null) {
                order.setStatus(updatedOrder.getStatus());
            }

            // Update order items if provided and not empty
            if (updatedOrder.getOrderItems() != null && !updatedOrder.getOrderItems().isEmpty()) {
                if (updatedOrder.getOrderItems().stream().anyMatch(item -> item.getProduct() == null || item.getProduct().getId() == null)) {
                    throw new RuntimeException("Sản phẩm trong đơn hàng không hợp lệ");
                }

                order.getOrderItems().clear();
                for (OrderItem item : updatedOrder.getOrderItems()) {
                    // Verify product existence
                    Product product = productRepository.findById(item.getProduct().getId())
                            .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

                    item.setOrder(order);
                    order.getOrderItems().add(item);
                }
            } else if (updatedOrder.getOrderItems() != null && updatedOrder.getOrderItems().isEmpty()) {
                throw new RuntimeException("Danh sách sản phẩm không hợp lệ");
            }

            return orderRepository.save(order);
        }).orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
    }
    @Override
    public void deleteOrder(Long id) {

        if (!orderRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy đơn hàng");
        }
        orderRepository.deleteById(id);
    }

    @Override
    public Order cancelOrder(Long orderId) {
        logger.info("Bắt đầu hủy đơn hàng với orderId: " + orderId);

        // Tìm đơn hàng
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // Kiểm tra trạng thái đơn hàng
        if (!order.getStatus().equals(OrderStatus.PENDING.name())) {
            throw new RuntimeException("Chỉ có thể hủy đơn hàng đang ở trạng thái PENDING");
        }

        // Cập nhật trạng thái thành CANCELLED
        order.setStatus(OrderStatus.CANCELED.name());

        // Khôi phục số lượng tồn kho cho từng sản phẩm trong đơn hàng
        for (OrderItem orderItem : order.getOrderItems()) {
            Product product = orderItem.getProduct();
            product.setStockQuantity(product.getStockQuantity() + orderItem.getQuantity());
            productRepository.save(product);
            logger.info("Đã khôi phục " + orderItem.getQuantity() + " sản phẩm " + product.getId() + " vào kho.");
        }

        // Lưu đơn hàng đã hủy
        Order cancelledOrder = orderRepository.save(order);
        logger.info("Đã hủy đơn hàng #" + orderId + " thành công.");
        return cancelledOrder;
    }

    @Override
    public MonthlyRevenueDTO getMonthlyRevenueWithOrdersStats(int month) {
        // Lấy doanh thu tháng
        List<Object[]> revenueStats = orderRepository.getMonthlyRevenue();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (Object[] stat : revenueStats) {
            Integer monthFromQuery = (Integer) stat[0]; // Lấy tháng từ query
            if (monthFromQuery == month) {
                totalRevenue = (BigDecimal) stat[1]; // Lấy tổng doanh thu cho tháng
                break;
            }
        }

        // Lấy danh sách đơn hàng của tháng
        List<Order> orders = orderRepository.findOrdersByMonth(month);

        // Trả về đối tượng MonthlyRevenueDTO
        return new MonthlyRevenueDTO(month, totalRevenue, orders);
    }


    @Override
    public Map<String, Object> getTopSellingProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        List<TopProductDTO> topProducts = orderRepository.findTop5BestSellingProducts(pageable);

        double totalRevenue = topProducts.stream()
                .mapToDouble(TopProductDTO::getRevenue)
                .sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("topSellingProducts", topProducts);
        stats.put("totalRevenue", totalRevenue);

        return stats;
    }


    @Override
    public MonthlyRevenueDTO getRevenueBetweenDates(LocalDateTime startDate, LocalDateTime endDate) {
        // Lấy tổng doanh thu trong khoảng thời gian
        BigDecimal totalRevenue = orderRepository.calculateRevenueBetweenDates(startDate, endDate)
                .orElse(BigDecimal.ZERO);

        // Lấy danh sách đơn hàng trong khoảng thời gian
        List<Order> orders = orderRepository.findOrdersBetweenDates(startDate, endDate);

        // Trả về DTO (có thể đặt lại tên DTO nếu không phù hợp với "Monthly")
        return new MonthlyRevenueDTO(0, totalRevenue, orders); // 0 nếu không cần "month"
    }


    // Phương thức lấy thống kê số lượng đơn hàng theo trạng thái
    @Override
    public Map<String, Object> getOrderStatusStats() {
        List<Order> allOrders = orderRepository.findAll();

        int pendingOrders = 0;
        int completedOrders = 0;
        int canceledOrders = 0;
        int CODOrders = 0;

        for (Order order : allOrders) {
            switch (order.getStatus()) {
                case "PENDING" -> pendingOrders++;
                case "PAID" -> completedOrders++;
                case "CANCELED" -> canceledOrders++;
                case "COD_CONFIRMED" -> CODOrders++;
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("pendingOrders", pendingOrders);
        stats.put("completedOrders", completedOrders);
        stats.put("canceledOrders", canceledOrders);
        stats.put("CODOrders", CODOrders);

        return stats;
    }
}
