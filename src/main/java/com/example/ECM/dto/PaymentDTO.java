package com.example.ECM.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDTO {
    private String paymentCode;
    private Long orderId;
    private int amount;
    private String status;
}
