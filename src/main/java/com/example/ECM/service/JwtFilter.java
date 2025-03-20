package com.example.ECM.service;

import com.example.ECM.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                chain.doFilter(request, response);
                return;
            }

            String token = authHeader.substring(7);
            String username = jwtUtil.extractUsername(token);
            String role = jwtUtil.extractRole(token);
            String phone = jwtUtil.extractPhone(token);
            String address = jwtUtil.extractAddress(token);
            String email = jwtUtil.extractEmail(token);
            String fullName = jwtUtil.extractFullName(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (jwtUtil.validateToken(token)) {
                    // Tạo Authentication token có thêm thông tin role, phone, address, email, fullName
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    // Gán thông tin bổ sung vào request để sử dụng sau này
                    request.setAttribute("role", role);
                    request.setAttribute("phone", phone);
                    request.setAttribute("address", address);
                    request.setAttribute("email", email);
                    request.setAttribute("fullName", fullName);
                }
            }

            chain.doFilter(request, response);
        } catch (Exception e) {
            // Xử lý các lỗi xảy ra trong quá trình xác thực JWT
            logger.error("Lỗi khi xử lý JWT: ", e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Lỗi xác thực token");
        }
    }

}
