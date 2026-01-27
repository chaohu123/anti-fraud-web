package com.xxx.antifraud.controller;

import com.xxx.antifraud.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket 消息控制器
 * 处理客户端发送的实时消息
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class    WebSocketController {

    private final WebSocketService webSocketService;

    /**
     * 处理客户端连接建立时的欢迎消息
     */
    @SubscribeMapping("/topic/notifications")
    public Map<String, Object> onSubscribeNotifications() {
        Map<String, Object> welcome = new HashMap<>();
        welcome.put("id", System.currentTimeMillis());
        welcome.put("title", "欢迎使用");
        welcome.put("message", "您已成功连接到反诈学习平台");
        welcome.put("type", "welcome");
        welcome.put("timestamp", System.currentTimeMillis());
        return welcome;
    }

    /**
     * 处理用户发送的ping消息，用于心跳检测
     */
    @MessageMapping("/ping")
    @SendTo("/topic/pong")
    public Map<String, Object> handlePing(@Payload Map<String, Object> ping, Principal principal) {
        log.debug("收到用户 {} 的ping消息: {}", principal != null ? principal.getName() : "anonymous", ping);

        Map<String, Object> pong = new HashMap<>();
        pong.put("timestamp", System.currentTimeMillis());
        pong.put("message", "pong");
        pong.put("user", principal != null ? principal.getName() : "anonymous");
        return pong;
    }

    /**
     * 处理用户状态更新消息
     */
    @MessageMapping("/user-status")
    public void handleUserStatus(@Payload Map<String, Object> status, Principal principal) {
        if (principal != null) {
            log.debug("用户 {} 状态更新: {}", principal.getName(), status);

            // 可以在这里处理用户在线状态等逻辑
            Map<String, Object> response = new HashMap<>();
            response.put("userId", principal.getName());
            response.put("status", status.get("status"));
            response.put("timestamp", System.currentTimeMillis());

            // 广播用户状态变更（可选）
            // webSocketService.sendToAll("/topic/user-status", response);
        }
    }

    /**
     * 处理学习进度同步消息
     */
    @MessageMapping("/learning-progress")
    public void handleLearningProgress(@Payload Map<String, Object> progress, Principal principal) {
        if (principal != null) {
            log.debug("用户 {} 学习进度更新: {}", principal.getName(), progress);

            // 这里可以触发实时通知给其他用户或管理员
            String knowledgeId = (String) progress.get("knowledgeId");
            Integer completed = (Integer) progress.get("completed");

            if (knowledgeId != null && completed != null && completed == 100) {
                // 发送学习完成通知
                webSocketService.sendLearningProgressNotification(
                    Long.parseLong(principal.getName()),
                    (String) progress.get("title"),
                    completed
                );
            }
        }
    }
}
