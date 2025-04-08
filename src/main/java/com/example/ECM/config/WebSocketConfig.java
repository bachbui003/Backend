package com.example.ECM.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic"); // Nơi server gửi tin nhắn tới
        config.setApplicationDestinationPrefixes("/app"); // Prefix client dùng để gửi tin nhắn tới server
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/chat")
                .setAllowedOriginPatterns("http://localhost:4200") // Cho phép mọi origin kết nối, nên hạn chế cụ thể hơn trong production
                .withSockJS(); // Dùng SockJS fallback khi WebSocket không hỗ trợ
    }
}
