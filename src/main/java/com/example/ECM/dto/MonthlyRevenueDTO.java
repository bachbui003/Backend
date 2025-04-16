package com.example.ECM.dto;

import com.example.ECM.model.Order;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class MonthlyRevenueDTO {
    private int month;
    private BigDecimal totalRevenue;
    private List<Order> orders;

    public MonthlyRevenueDTO(int month, BigDecimal totalRevenue, List<Order> orders) {
        this.month = month;
        this.totalRevenue = totalRevenue;
        this.orders = orders;
    }
}
