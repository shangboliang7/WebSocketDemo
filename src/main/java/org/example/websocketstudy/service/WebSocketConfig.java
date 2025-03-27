package org.example.websocketstudy.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // 全局会话管理器（线程安全）
    private static final ConcurrentHashMap<String, WebSocketSessionWrapper> activeSessions = new ConcurrentHashMap<>();

    // 全局定时任务线程池
    private static final ScheduledExecutorService globalScheduler = Executors.newSingleThreadScheduledExecutor();

    // 心跳参数配置
    private static final long HEARTBEAT_INTERVAL = 10; // 心跳间隔（秒）
    private static final long HEARTBEAT_TIMEOUT = 1000;  // 超时时间（秒）

    static {
        // 启动全局心跳检查任务
        globalScheduler.scheduleAtFixedRate(
                WebSocketConfig::checkAllSessions,
                HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.SECONDS
        );
    }


    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 客户端订阅消息的前缀（服务端向客户端推送消息的路径前缀）
        config.enableSimpleBroker("/topic"); //心跳监测
        //  前端调用需要加的前缀
        config.setApplicationDestinationPrefixes("/app");
        //两个区别很大 一个是识别/topic路径 一个是新增/app路径
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 连接端点
        registry.addEndpoint("/chat").setAllowedOrigins("http://localhost:63342/").withSockJS();

    }




    // ================ 会话管理方法 ================
    private static void registerSession(WebSocketSession session) {
        activeSessions.put(session.getId(), new WebSocketSessionWrapper(session));
        System.out.println("Session registered: " + session.getId());
    }
    private static void unregisterSession(WebSocketSession session) {
        WebSocketSessionWrapper wrapper = activeSessions.remove(session.getId());
        if (wrapper != null) {
            wrapper.close(); // 清理资源
        }
        System.out.println("Session unregistered: " + session.getId());
    }

    private static void updateSessionPongTime(WebSocketSession session) {
        WebSocketSessionWrapper wrapper = activeSessions.get(session.getId());
        if (wrapper != null) {
            wrapper.updateLastPongTime();
        }
    }

    // ================ 全局心跳检查 ================
    private static void checkAllSessions() {
        long currentTime = System.currentTimeMillis();
        System.out.println("开始心跳建");
        activeSessions.forEach((id, wrapper) -> {
            WebSocketSession session = wrapper.getSession();
            try {
                // 发送心跳Ping
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage("心跳监测"));
                    System.out.println("Sent ping to session: " + id);
                }

                // 检查超时
                long timeSinceLastPong = currentTime - wrapper.getLastPongTime();
                if (timeSinceLastPong > HEARTBEAT_TIMEOUT * 1000) {
                    System.out.println("Closing expired session: " + id);
                    session.close(CloseStatus.SESSION_NOT_RELIABLE);
                }
            } catch (IOException e) {
                System.out.println("Error checking session " + id + ": " + e.getMessage());
            }
        });
    }
    // ================ 会话包装类 ================
    private static class WebSocketSessionWrapper {
        private final WebSocketSession session;
        private volatile long lastPongTime;

        public WebSocketSessionWrapper(WebSocketSession session) {
            this.session = session;
            this.lastPongTime = System.currentTimeMillis(); // 初始化为连接时间
        }

        public void updateLastPongTime() {
            this.lastPongTime = System.currentTimeMillis();
        }

        public long getLastPongTime() {
            return lastPongTime;
        }

        public WebSocketSession getSession() {
            return session;
        }

        public void close() {
            try {
                if (session.isOpen()) {
                    session.close();
                }
            } catch (IOException e) {
                System.out.println("Error closing session: " + e.getMessage());
            }
        }
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.addDecoratorFactory(new WebSocketHandlerDecoratorFactory() {
            @Override
            public WebSocketHandler decorate(WebSocketHandler handler) {
                return new WebSocketHandlerDecorator(handler) {
                    @Override
                    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                        // 注册新会话
                        registerSession(session);
                        super.afterConnectionEstablished(session);
                    }

                    @Override
                    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                        // 注销会话
                        unregisterSession(session);
                        super.afterConnectionClosed(session, closeStatus);
                    }

                    @Override
                    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
                        // 只处理 Pong 消息
                        updateSessionPongTime(session);
                        System.out.println("Received pong from session: " + session.getId());
                        super.handleMessage(session, message);
                    }
                };
            }
        });
    }



}
