package org.example.websocketstudy.service;

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
        // 客户端订阅消息的前缀（服务端向客户端推送消息的路径前缀）
        config.enableSimpleBroker("/topic");//心跳监测
        //  前端调用需要加的前缀
        config.setApplicationDestinationPrefixes("/app");
        //两个区别很大 一个是识别/topic路径 一个是新增/app路径
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 连接端点
        registry.addEndpoint("/chat").setAllowedOrigins("http://localhost:63342/").withSockJS();

    }


}
