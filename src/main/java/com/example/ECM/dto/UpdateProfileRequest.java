package com.example.ECM.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {
    private String email;
    private String fullName;
    private String phone;
    private String address;
}
