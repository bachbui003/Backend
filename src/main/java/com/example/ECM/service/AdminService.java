package com.example.ECM.service;



import com.example.ECM.exception.AdminNotFoundException;
import com.example.ECM.model.Admin;
import com.example.ECM.repository.AdminRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AdminService {
    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);
    private final AdminRepository adminRepository;

    public AdminService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    // 📌 Tìm Admin theo Email
    public Optional<Admin> findAdminByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email không được để trống");
        }
        return adminRepository.findByEmail(email);
    }

    // 📌 Tìm Admin theo Username
    public Optional<Admin> findAdminByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username không được để trống");
        }
        return adminRepository.findByUsername(username);
    }

    // 📌 Lấy danh sách tất cả Admin
    public List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }

    // 📌 Thêm mới một Admin
    public Admin createAdmin(Admin admin) {
        if (adminRepository.findByEmail(admin.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email này đã tồn tại!");
        }
        return adminRepository.save(admin);
    }

    // 📌 Cập nhật thông tin Admin
    public Admin updateAdmin(Long id, Admin updatedAdmin) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new AdminNotFoundException("Không tìm thấy admin với ID: " + id));

        admin.setUsername(updatedAdmin.getUsername());
        admin.setEmail(updatedAdmin.getEmail());
        admin.setPassword(updatedAdmin.getPassword());

        return adminRepository.save(admin);
    }

    // 📌 Xóa Admin theo ID
    public void deleteAdmin(Long id) {
        if (!adminRepository.existsById(id)) {
            throw new AdminNotFoundException("Không tìm thấy admin với ID: " + id);
        }
        adminRepository.deleteById(id);
    }
}
