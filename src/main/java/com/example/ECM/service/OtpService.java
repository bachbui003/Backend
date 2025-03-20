package com.example.ECM.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class OtpService {
    private final Map<String, String> otpStorage = new HashMap<>();
    private final Random random = new Random();

    public String generateOtp(String email) {
        try {
            String otp = String.format("%06d", random.nextInt(1000000));
            otpStorage.put(email, otp);
            return otp;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo OTP cho email " + email + ": " + e.getMessage(), e);
        }
    }

    public boolean validateOtp(String email, String otp) {
        try {
            String storedOtp = otpStorage.get(email);
            if (storedOtp == null) {
                throw new RuntimeException("Không tìm thấy OTP cho email " + email);
            }
            return otp.equals(storedOtp);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xác thực OTP cho email " + email + ": " + e.getMessage(), e);
        }
    }

    public void clearOtp(String email) {
        try {
            otpStorage.remove(email);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xóa OTP cho email " + email + ": " + e.getMessage(), e);
        }
    }
}
