package com.example.ECM.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Sử dụng một message broker để quản lý các tin nhắn
        config.enableSimpleBroker("/topic"); // Định tuyến tin nhắn đến các clients
        config.setApplicationDestinationPrefixes("/app"); // Định tuyến yêu cầu từ client đến server
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Đăng ký endpoint cho WebSocket
        registry.addEndpoint("/chat").withSockJS(); // Chỉ định endpoint cho WebSocket
    }
}
