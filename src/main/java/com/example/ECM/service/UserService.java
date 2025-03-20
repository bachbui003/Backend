package com.example.ECM.service;

import com.example.ECM.model.User;
import com.example.ECM.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    // 🔹 Cập nhật user (chỉ update nếu user tồn tại)
    public Optional<User> updateUser(Long id, User updatedUser) {
        try {
            return userRepository.findById(id).map(existingUser -> {
                existingUser.setFullName(updatedUser.getFullName());
                existingUser.setEmail(updatedUser.getEmail());
                existingUser.setPhone(updatedUser.getPhone());
                existingUser.setAddress(updatedUser.getAddress());
                existingUser.setRole(updatedUser.getRole());
                return userRepository.save(existingUser);
            });
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi cập nhật user: " + e.getMessage(), e);
        }
    }

    // 🔹 Xóa user
    public boolean deleteUser(Long id) {
        try {
            if (userRepository.existsById(id)) {
                userRepository.deleteById(id);
                return true;
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xóa user: " + e.getMessage(), e);
        }
    }
}
