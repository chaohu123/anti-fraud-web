package com.xxx.antifraud.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket 消息服务
 * 提供实时消息推送功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 发送广播消息到所有订阅的用户
     */
    public void sendToAll(String destination, Object message) {
        messagingTemplate.convertAndSend(destination, message);
        log.debug("发送广播消息到 {}: {}", destination, message);
    }

    /**
     * 发送消息到指定用户
     */
    public void sendToUser(String userId, String destination, Object message) {
        messagingTemplate.convertAndSendToUser(userId, destination, message);
        log.debug("发送消息到用户 {} 的 {}: {}", userId, destination, message);
    }

    /**
     * 发送系统通知
     */
    public void sendSystemNotification(String title, String message, String type) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("id", System.currentTimeMillis());
        notification.put("title", title);
        notification.put("message", message);
        notification.put("type", type);
        notification.put("timestamp", System.currentTimeMillis());

        sendToAll("/topic/notifications", notification);
    }

    /**
     * 发送用户成就解锁通知
     */
    public void sendAchievementNotification(Long userId, String achievementName, String description) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("id", System.currentTimeMillis());
        notification.put("title", "🎉 成就解锁！");
        notification.put("message", String.format("恭喜您解锁成就：%s", achievementName));
        notification.put("description", description);
        notification.put("type", "achievement");
        notification.put("timestamp", System.currentTimeMillis());

        sendToUser(userId.toString(), "/queue/notifications", notification);
    }

    /**
     * 发送风险等级变更通知
     */
    public void sendRiskLevelChangeNotification(Long userId, String oldLevel, String newLevel, double score) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("id", System.currentTimeMillis());
        notification.put("title", "📊 风险评估更新");
        notification.put("message", String.format("您的风险等级从 %s 变更为 %s", oldLevel, newLevel));
        notification.put("score", score);
        notification.put("oldLevel", oldLevel);
        notification.put("newLevel", newLevel);
        notification.put("type", "risk-update");
        notification.put("timestamp", System.currentTimeMillis());

        sendToUser(userId.toString(), "/queue/notifications", notification);
    }

    /**
     * 发送学习进度更新通知
     */
    public void sendLearningProgressNotification(Long userId, String knowledgeTitle, int progress) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("id", System.currentTimeMillis());
        notification.put("title", "📚 学习进度更新");
        notification.put("message", String.format("您已完成学习：%s", knowledgeTitle));
        notification.put("progress", progress);
        notification.put("type", "learning");
        notification.put("timestamp", System.currentTimeMillis());

        sendToUser(userId.toString(), "/queue/notifications", notification);
    }

    /**
     * 发送训练完成通知
     */
    public void sendTrainingCompleteNotification(Long userId, boolean isCorrect, String scamType) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("id", System.currentTimeMillis());
        notification.put("title", isCorrect ? "✅ 训练成功" : "❌ 训练待改进");
        notification.put("message", String.format("您%s识别了%s类型的诈骗", isCorrect ? "正确" : "未正确", scamType));
        notification.put("isCorrect", isCorrect);
        notification.put("scamType", scamType);
        notification.put("type", "training");
        notification.put("timestamp", System.currentTimeMillis());

        sendToUser(userId.toString(), "/queue/notifications", notification);
    }
}