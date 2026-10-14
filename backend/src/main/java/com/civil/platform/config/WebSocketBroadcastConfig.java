package com.civil.platform.config;

import com.civil.platform.module.websocket.GlobalBroadcastWebSocketHandler;
import com.civil.platform.module.websocket.ClassroomChatWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketBroadcastConfig implements WebSocketConfigurer {

    private final GlobalBroadcastWebSocketHandler globalBroadcastWebSocketHandler;
    private final ClassroomChatWebSocketHandler classroomChatWebSocketHandler;

    public WebSocketBroadcastConfig(GlobalBroadcastWebSocketHandler globalBroadcastWebSocketHandler,
                                    ClassroomChatWebSocketHandler classroomChatWebSocketHandler) {
        this.globalBroadcastWebSocketHandler = globalBroadcastWebSocketHandler;
        this.classroomChatWebSocketHandler = classroomChatWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(globalBroadcastWebSocketHandler, "/ws/broadcast")
                .setAllowedOrigins("*");
        registry.addHandler(classroomChatWebSocketHandler, "/ws/classroom-chat")
                .setAllowedOrigins("*");
    }
}
