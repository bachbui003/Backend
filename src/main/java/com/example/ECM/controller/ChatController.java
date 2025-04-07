package com.example.ECM.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/sendMessage")
    public void sendMessage(String message) {
        // Gửi tin nhắn đến tất cả các clients đang kết nối
        messagingTemplate.convertAndSend("/topic/messages", message);
    }
}
