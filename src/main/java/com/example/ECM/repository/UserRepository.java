package com.example.ECM.repository;

import com.example.ECM.model.Role;
import com.example.ECM.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    // ✅ Cập nhật phương thức để tìm bằng username hoặc email
    Optional<User> findByUsernameOrEmail(String username, String email);
    List<User> findByRole(Role role); // Tìm tất cả user có role cụ thể
}
