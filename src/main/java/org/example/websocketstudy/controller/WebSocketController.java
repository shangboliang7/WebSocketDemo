package org.example.websocketstudy.controller;

/**
 * @author sunhao
 * @date 2025/3/26
 */
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    // 处理客户端发送到 /app/chat 的消息
    @MessageMapping("/chat")
    // 将返回结果广播到 /topic/messages
    @SendTo("/topic/messages")
    public String handleMessage(String message) {
        System.out.println("收到消息: " + message);
        return "服务端回复: " + message;
    }
}


