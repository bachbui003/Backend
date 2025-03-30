package com.example.ECM.model;


public enum VNPayResponseCode {
    SUCCESS("00"),          // Giao dịch thành công
    USER_CANCELLED("24"),   // Người dùng hủy giao dịch
    TRANSACTION_FAILED("97"), // Giao dịch thất bại
    INVALID_SIGNATURE("99"), // Chữ ký không hợp lệ
    UNKNOWN("");            // Mã không xác định

    private final String code;

    VNPayResponseCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static VNPayResponseCode fromCode(String code) {
        for (VNPayResponseCode response : VNPayResponseCode.values()) {
            if (response.code.equals(code)) {
                return response;
            }
        }
        return UNKNOWN;
    }
}
