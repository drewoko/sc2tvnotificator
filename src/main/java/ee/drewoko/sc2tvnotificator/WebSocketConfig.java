package ee.drewoko.sc2tvnotificator;

import ee.drewoko.sc2tvnotificator.web.ListenWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry webSocketHandlerRegistry) {

        webSocketHandlerRegistry.addHandler(listenWebSocketHandler(), "/listen").withSockJS();

    }

    @Bean
    public WebSocketHandler listenWebSocketHandler() {
        return new ListenWebSocketHandler();
    }

}