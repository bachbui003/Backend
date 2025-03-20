package com.example.ECM.repository;


import com.example.ECM.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    boolean existsByUserIdAndStatus(Long userId, String status);

}