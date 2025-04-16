package com.example.ECM.service;

import com.example.ECM.dto.MonthlyRevenueDTO;
import com.example.ECM.model.Order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface OrderService {

    // Tạo đơn hàng mới
    Order createOrder(Long userId, List<Long> selectedCartItemIds);

    // Lấy thông tin đơn hàng theo ID
    Order getOrderById(Long id);

    // Lấy danh sách đơn hàng của một người dùng
    List<Order> getOrdersByUserId(Long userId);

    // Lấy tất cả đơn hàng
    List<Order> getAllOrders();

    // Cập nhật đơn hàng (bao gồm trạng thái và chi tiết đơn hàng)
    Order updateOrder(Long id, Order updatedOrder);

    // Xóa đơn hàng
    void deleteOrder(Long id);

    // Hủy đơn hàng
    Order cancelOrder(Long orderId);

    // Phương thức lấy thống kê doanh thu và danh sách đơn hàng theo tháng
    MonthlyRevenueDTO getMonthlyRevenueWithOrdersStats(int month);

    // Phương thức lấy thống kê sản phẩm bán chạy
    Map<String, Object> getTopSellingProducts();

    // Phương thức lấy thống kê số lượng đơn hàng theo trạng thái
    Map<String, Object> getOrderStatusStats();
    MonthlyRevenueDTO getRevenueBetweenDates(LocalDateTime startDate, LocalDateTime endDate);


}