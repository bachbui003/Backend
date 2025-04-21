package com.example.ECM.service;

import com.example.ECM.dto.*;
import com.example.ECM.model.Role;
import com.example.ECM.model.User;
import com.example.ECM.repository.UserRepository;
import com.example.ECM.util.JwtUtil;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Transactional
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final OtpService otpService;
    private final JwtUtil jwtUtil;

    @Autowired
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService, OtpService otpService, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.otpService = otpService;
        this.jwtUtil = jwtUtil;
    }



    public String register(RegisterRequest request) {

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return "Tên người dùng đã tồn tại";
        }


        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        if (!Pattern.matches(emailRegex, request.getEmail())) {
            return "Email không hợp lệ";
        }


        String phoneRegex = "^(\\+\\d{1,3}[- ]?)?\\d{10}$"; // Kiểm tra số điện thoại có thể bắt đầu với mã vùng quốc tế
        if (!Pattern.matches(phoneRegex, request.getPhone())) {
            return "Số điện thoại không hợp lệ";
        }


        String passwordRegex = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        if (!Pattern.matches(passwordRegex, request.getPassword())) {
            return "Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, chữ số và ký tự đặc biệt";
        }


        Role role = (request.getRole() != null) ? request.getRole() : Role.USER;
        if (role != Role.USER && role != Role.ADMIN) {
            return "Role không hợp lệ. Giá trị hợp lệ: USER, ADMIN";
        }


        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setPhone(request.getPhone());
        user.setFullName(request.getFullName());

        userRepository.save(user);

        // Gửi email thông báo đăng ký thành công
        String subject = "Đăng ký tài khoản thành công";
        String content = "Xin chào " + user.getFullName() + ",\n\n"
                + "Bạn đã đăng ký tài khoản thành công trên hệ thống của chúng tôi.\n"
                + "Chúc bạn trải nghiệm mua sắm vui vẻ!\n\n"
                + "Trân trọng,\nĐội ngũ hỗ trợ ECM.";
        emailService.sendEmail(user.getEmail(), subject, content);

        logger.info("Người dùng {} đã đăng ký thành công với role {}", request.getUsername(), role);
        return "Đăng ký thành công";
    }


    public AuthResponse login(AuthRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            logger.warn("Đăng nhập thất bại - Thiếu username hoặc email");
            return new AuthResponse("Username hoặc Email là bắt buộc", null);
        }

        // Tìm người dùng theo username hoặc email
        User user = userRepository.findByUsername(request.getUsername())
                .or(() -> userRepository.findByEmail(request.getUsername()))
                .orElseThrow(() -> new RuntimeException("Username hoặc mật khẩu không hợp lệ"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            logger.warn("Đăng nhập thất bại - Mật khẩu không chính xác cho người dùng: {}", request.getUsername());
            return new AuthResponse("Username hoặc mật khẩu không hợp lệ", "Đăng nhập thất bại");
        }

        // Thêm thông tin role, phone, address vào JWT
        String token = jwtUtil.generateToken(user.getUsername(), user.getId(), user.getRole().name(), user.getPhone(), user.getAddress(), user.getEmail(), user.getFullName());

        logger.info("Người dùng {} đã đăng nhập thành công", request.getUsername());
        return new AuthResponse(token, "Đăng nhập thành công");
    }

    public String forgotPassword(ForgotPasswordRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            return "Email không tồn tại trong hệ thống.";
        }

        String otp = otpService.generateOtp(request.getEmail());
        emailService.sendEmail(request.getEmail(), "Mã OTP đặt lại mật khẩu", "Mã OTP của bạn: " + otp);
        logger.info("OTP đã được gửi đến email: {}", request.getEmail());

        return "OTP đã được gửi đến email.";
    }

    public String resetPassword(ResetPasswordRequest request) {
        // Kiểm tra OTP
        if (!otpService.validateOtp(request.getEmail(), request.getOtp())) {
            return "OTP không hợp lệ.";
        }

        // Lấy thông tin người dùng từ email
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();


        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            return "Mật khẩu mới không được trùng với mật khẩu cũ.";
        }


        if (request.getNewPassword().length() < 8) {
            return "Mật khẩu mới phải có ít nhất 6 ký tự.";
        }

        if (!isStrongPassword(request.getNewPassword())) {
            return "Mật khẩu mới phải bao gồm ít nhất một chữ cái, một chữ số và một ký tự đặc biệt.";
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Xóa OTP đã sử dụng
        otpService.clearOtp(request.getEmail());

        // Gửi email thông báo đặt lại mật khẩu thành công
        String subject = "Mật khẩu của bạn đã được đặt lại thành công";
        String content = "Xin chào " + user.getFullName() + ",\n\n"
                + "Mật khẩu của bạn đã được đặt lại thành công trên hệ thống của chúng tôi.\n"
                + "Nếu bạn không thực hiện hành động này, vui lòng liên hệ với chúng tôi ngay lập tức.\n\n"
                + "Trân trọng,\nĐội ngũ hỗ trợ ECM.";
        emailService.sendEmail(user.getEmail(), subject, content);


        logger.info("Đặt lại mật khẩu thành công cho người dùng với email: {}", request.getEmail());
        return "Mật khẩu đã được đặt lại thành công.";
    }

    // Hàm kiểm tra mật khẩu mạnh
    private boolean isStrongPassword(String password) {
        // Kiểm tra mật khẩu có ít nhất một chữ cái, một chữ số và một ký tự đặc biệt
        String regex = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(password).matches();
    }
}
