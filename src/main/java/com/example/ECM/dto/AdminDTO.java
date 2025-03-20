package com.example.ECM.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdminDTO {
    String name;
    String password;
    String email;
    String phone;
    String address;
    String fullName;

}
