package com.example.ECM.service;

import com.example.ECM.model.Order;

import java.util.List;

public interface OrderService {
    Order createOrder(Long userId, List<Long> selectedCartItemIds); // Cập nhật để đặt hàng với sản phẩm được chọn
    Order getOrderById(Long id);
    List<Order> getOrdersByUserId(Long userId);
    List<Order> getAllOrders();
    Order updateOrder(Long id, Order updatedOrder);
    void deleteOrder(Long id);
//    Order saveOrder(Order order); // Phương thức lưu đơn hàng
    Order updateOrderStatus(Long orderId, String status); // Phương thức cập nhật trạng thái đơn hàng
}
