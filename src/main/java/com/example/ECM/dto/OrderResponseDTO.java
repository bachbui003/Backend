package com.example.ECM.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponseDTO {
    private Long id;
    private String username;
    private String email;
    private BigDecimal totalPrice;
    private String status;
    private List<OrderItemDTO> orderItems;
}
