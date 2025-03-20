package com.example.ECM.controller;

import com.example.ECM.dto.*;
import com.example.ECM.model.Role;
import com.example.ECM.model.User;
import com.example.ECM.repository.UserRepository;
import com.example.ECM.service.EmailService;
import com.example.ECM.service.OtpService;
import com.example.ECM.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
@CrossOrigin(origins = "http://localhost:4200") // Cho phép Angular gọi API
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final OtpService otpService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest request) {
        try {
            if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                return ResponseEntity.badRequest().body("Tên người dùng đã tồn tại");
            }
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                return ResponseEntity.badRequest().body("Email đã tồn tại");
            }

            // Nếu không gửi role, mặc định là USER
            Role role = (request.getRole() != null) ? request.getRole() : Role.USER;

            // Kiểm tra role hợp lệ
            if (role != Role.USER && role != Role.ADMIN) {
                return ResponseEntity.badRequest().body("Role không hợp lệ. Giá trị hợp lệ: USER, ADMIN");
            }

            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole(role);
            user.setPhone(request.getPhone());
            user.setAddress(request.getAddress());
            user.setFullName(request.getFullName());

            userRepository.save(user);

            logger.info("Người dùng {} đã đăng ký thành công với role {}", request.getUsername(), role);
            return ResponseEntity.ok("Người dùng đăng ký thành công với role: " + role);

        } catch (Exception e) {
            logger.error("Lỗi trong quá trình đăng ký người dùng: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Đã có lỗi xảy ra trong quá trình đăng ký người dùng.");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid AuthRequest request) {
        try {
            logger.info("Nhận yêu cầu đăng nhập: username={}", request.getUsername());

            // Kiểm tra username/email có hợp lệ không
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                logger.warn("Đăng nhập thất bại - Thiếu username hoặc email");
                return ResponseEntity.badRequest().body(new AuthResponse("Username hoặc Email là bắt buộc", null));
            }

            // Tìm người dùng theo username hoặc email
            User user = userRepository.findByUsername(request.getUsername())
                    .or(() -> userRepository.findByEmail(request.getUsername()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Username hoặc mật khẩu không hợp lệ"));

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                logger.warn("Đăng nhập thất bại - Mật khẩu không chính xác cho người dùng: {}", request.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse("Username hoặc mật khẩu không hợp lệ", "Đăng nhập thất bại"));
            }

            // Thêm thông tin role, phone, address vào JWT
            String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name(), user.getPhone(), user.getAddress(), user.getEmail(), user.getFullName());

            logger.info("Người dùng {} đã đăng nhập thành công", request.getUsername());
            return ResponseEntity.ok(new AuthResponse(token, "Đăng nhập thành công"));

        } catch (Exception e) {
            logger.error("Lỗi trong quá trình đăng nhập: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AuthResponse("Đã có lỗi xảy ra trong quá trình đăng nhập", null));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Email không tồn tại trong hệ thống.");
            }

            String otp = otpService.generateOtp(request.getEmail());
            emailService.sendEmail(request.getEmail(), "Mã OTP đặt lại mật khẩu", "Mã OTP của bạn: " + otp);
            logger.info("OTP đã được gửi đến email: {}", request.getEmail());

            return ResponseEntity.ok("OTP đã được gửi đến email.");
        } catch (Exception e) {
            logger.error("Lỗi trong quá trình quên mật khẩu: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Đã có lỗi xảy ra trong quá trình quên mật khẩu.");
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        try {
            if (!otpService.validateOtp(request.getEmail(), request.getOtp())) {
                return ResponseEntity.badRequest().body("OTP không hợp lệ.");
            }

            User user = userRepository.findByEmail(request.getEmail()).orElseThrow();

            if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
                return ResponseEntity.badRequest().body("Mật khẩu mới không được trùng với mật khẩu cũ.");
            }

            if (request.getNewPassword().length() < 6) {
                return ResponseEntity.badRequest().body("Mật khẩu mới phải có ít nhất 6 ký tự.");
            }

            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);
            otpService.clearOtp(request.getEmail());

            logger.info("Đặt lại mật khẩu thành công cho người dùng với email: {}", request.getEmail());
            return ResponseEntity.ok("Mật khẩu đã được đặt lại thành công.");
        } catch (Exception e) {
            logger.error("Lỗi trong quá trình reset mật khẩu: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Đã có lỗi xảy ra trong quá trình reset mật khẩu.");
        }
    }
}
