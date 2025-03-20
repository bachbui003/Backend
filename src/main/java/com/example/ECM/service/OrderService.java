package com.example.ECM.service;

import com.example.ECM.model.Order;

import java.util.List;

public interface OrderService {
    Order createOrder(Long userId);
    Order getOrderById(Long id);
    List<Order> getOrdersByUserId(Long userId);
    List<Order> getAllOrders();
    Order updateOrder(Long id, Order updatedOrder);
    void deleteOrder(Long id);
    Order saveOrder(Order order); // Thêm phương thức lưu đơn hàng
    Order updateOrderStatus(Long orderId, String status); // Thêm phương thức cập nhật trạng thái đơn hàng
}
