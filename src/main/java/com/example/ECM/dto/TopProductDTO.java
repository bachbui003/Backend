package com.example.ECM.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TopProductDTO {
    private String productName;
    private Long quantitySold;
    private Double price; // thêm đơn giá
    private Double revenue; // hoặc tính sẵn revenue = quantitySold * price

    public TopProductDTO(String productName, Long quantitySold, Double price) {
        this.productName = productName;
        this.quantitySold = quantitySold;
        this.price = price;
        this.revenue = quantitySold * price;
    }
}

