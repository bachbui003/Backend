package com.example.ECM.repository;


import com.example.ECM.dto.MonthlyRevenueDTO;
import com.example.ECM.dto.TopProductDTO;
import com.example.ECM.model.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    boolean existsByUserIdAndStatus(Long userId, String status);


    // Query lấy doanh thu theo tháng
    @Query("SELECT MONTH(o.orderDate), SUM(o.totalPrice) " +
            "FROM Order o WHERE o.status IN ('PENDING', 'COD_CONFIRMED', 'PAID', 'CANCELED') " +
            "GROUP BY MONTH(o.orderDate) ORDER BY MONTH(o.orderDate)")
    List<Object[]> getMonthlyRevenue();

    // Query lấy danh sách đơn hàng theo tháng
    @Query("SELECT o FROM Order o WHERE MONTH(o.orderDate) = :month AND o.status IN ('PENDING', 'COD_CONFIRMED', 'PAID', 'CANCELED')")
    List<Order> findOrdersByMonth(@Param("month") int month);

    @Query("SELECT new com.example.ECM.dto.TopProductDTO(p.name, SUM(oi.quantity), p.price) " +
            "FROM OrderItem oi JOIN oi.product p " +
            "GROUP BY p.name, p.price " +
            "ORDER BY SUM(oi.quantity) DESC")
    List<TopProductDTO> findTop10BestSellingProducts(Pageable pageable);

    @Query("SELECT SUM(o.totalPrice) FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate AND o.status NOT IN ('PENDING', 'CANCELED')")
    Optional<BigDecimal> calculateRevenueBetweenDates(@Param("startDate") LocalDateTime startDate,
                                                      @Param("endDate") LocalDateTime endDate);

    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate AND o.status NOT IN ('PENDING', 'CANCELED')")
    List<Order> findOrdersBetweenDates(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);


}