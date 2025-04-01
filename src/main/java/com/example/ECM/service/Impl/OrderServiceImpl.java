package com.example.ECM.service.Impl;

import com.example.ECM.model.*;
import com.example.ECM.repository.*;
import com.example.ECM.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime; // Thêm import cho LocalDateTime
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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
        newOrder.setOrderDate(LocalDateTime.now()); // Thiết lập orderDate
        newOrder.setStatus(OrderStatus.PENDING.name());

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (CartItem cartItem : selectedCartItems) {
            Product product = cartItem.getProduct();
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException("Sản phẩm " + product.getName() + " không đủ hàng!");
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(newOrder);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(BigDecimal.valueOf(product.getPrice()).multiply(BigDecimal.valueOf(cartItem.getQuantity())));
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
            if (updatedOrder.getOrderItems() == null || updatedOrder.getOrderItems().isEmpty()) {
                throw new RuntimeException("Danh sách sản phẩm không hợp lệ");
            }
            order.setStatus(updatedOrder.getStatus());
            order.getOrderItems().clear();
            for (OrderItem item : updatedOrder.getOrderItems()) {
                item.setOrder(order);
                order.getOrderItems().add(item);
            }
            return orderRepository.save(order);
        }).orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
    }

    @Override
    public Order updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        order.setStatus(status);
        return orderRepository.save(order);
    }

    @Override
    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy đơn hàng");
        }
        orderRepository.deleteById(id);
    }
}