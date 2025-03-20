package com.example.ECM.service.Impl;

import com.example.ECM.model.*;
import com.example.ECM.repository.CartRepository;
import com.example.ECM.repository.CartItemRepository;
import com.example.ECM.repository.OrderItemRepository;
import com.example.ECM.repository.OrderRepository;
import com.example.ECM.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository, CartRepository cartRepository,
                            CartItemRepository cartItemRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    public Order createOrder(Long userId) {
        logger.info("Bắt đầu tạo đơn hàng cho userId: " + userId);

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giỏ hàng"));

        if (cart.getCartItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng trống, không thể đặt hàng!");
        }

        Order newOrder = new Order();
        newOrder.setUser(cart.getUser());
        newOrder.setStatus(OrderStatus.PENDING.name());

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getCartItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(newOrder);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(BigDecimal.valueOf(cartItem.getProduct().getPrice())
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            orderItems.add(orderItem);
            totalPrice = totalPrice.add(orderItem.getPrice());
        }

        newOrder.setTotalPrice(totalPrice);
        newOrder.setOrderItems(orderItems);
        Order savedOrder = orderRepository.save(newOrder);

        // Xóa toàn bộ sản phẩm trong giỏ hàng
        cartItemRepository.deleteAll(cart.getCartItems());
        logger.info("Đã xóa sản phẩm trong giỏ hàng sau khi đặt hàng.");

        // Xóa giỏ hàng luôn
        cartRepository.delete(cart);
        logger.info("Đã xóa giỏ hàng sau khi đặt hàng.");

        return savedOrder;
    }


    @Override
    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Order not found"));
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
        }).orElseThrow(() -> new RuntimeException("Order not found"));
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
            throw new RuntimeException("Order not found");
        }
        orderRepository.deleteById(id);
    }

    @Override
    public Order saveOrder(Order order) {
        return null;
    }
}
