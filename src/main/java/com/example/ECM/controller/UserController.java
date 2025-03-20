package com.example.ECM.controller;

import com.example.ECM.dto.UpdateProfileRequest;
import com.example.ECM.model.User;
import com.example.ECM.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
@CrossOrigin(origins = "http://localhost:4200") // Cho phép Angular gọi API
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserRepository userRepository;

    // ✅ Lấy thông tin của chính người dùng đang đăng nhập
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            return userRepository.findByUsername(userDetails.getUsername())
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Đã xảy ra lỗi khi lấy thông tin người dùng: " + e.getMessage());
        }
    }

    // ✅ ADMIN lấy danh sách tất cả người dùng
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Đã xảy ra lỗi khi lấy danh sách người dùng: " + e.getMessage());
        }
    }

    // ✅ Cập nhật thông tin cá nhân
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                           @RequestBody UpdateProfileRequest request) {
        try {
            if (request == null) {
                return ResponseEntity.badRequest().body("Invalid request");
            }

            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            user.setEmail(request.getEmail());
            user.setFullName(request.getFullName());
            user.setPhone(request.getPhone());
            user.setAddress(request.getAddress());

            userRepository.save(user);

            return ResponseEntity.ok("Cập nhật thông tin thành công.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body("Người dùng không tồn tại: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Đã xảy ra lỗi khi cập nhật thông tin người dùng: " + e.getMessage());
        }
    }

    // ✅ ADMIN có thể cập nhật thông tin người khác
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UpdateProfileRequest request) {
        try {
            if (request == null) {
                return ResponseEntity.badRequest().body("Invalid request");
            }

            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            user.setEmail(request.getEmail());
            user.setFullName(request.getFullName());
            user.setPhone(request.getPhone());
            user.setAddress(request.getAddress());

            userRepository.save(user);
            return ResponseEntity.ok("Admin đã cập nhật thông tin người dùng thành công.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body("Người dùng không tồn tại: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Đã xảy ra lỗi khi cập nhật thông tin người dùng: " + e.getMessage());
        }
    }

    // ✅ ADMIN có thể xóa user
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            if (!userRepository.existsById(id)) {
                return ResponseEntity.badRequest().body("User not found");
            }

            userRepository.deleteById(id);
            return ResponseEntity.ok("User đã bị xóa.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Đã xảy ra lỗi khi xóa người dùng: " + e.getMessage());
        }
    }
}
