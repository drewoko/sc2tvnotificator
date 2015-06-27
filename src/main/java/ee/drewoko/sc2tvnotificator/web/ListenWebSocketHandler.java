package ee.drewoko.sc2tvnotificator.web;

import ee.drewoko.sc2tvnotificator.core.SessionRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.*;

import java.util.ArrayList;

/**
 * Created by Deniss Gubanov on 15/03/15.
 * Project: sc2tvnotificator
 * Package: ee.drewoko.sc2tvnotificator.web
 */
public class ListenWebSocketHandler implements WebSocketHandler {

    @Autowired
    SessionRepository sessionRepository;

    @Override
    public void afterConnectionEstablished(WebSocketSession webSocketSession) throws Exception {

        webSocketSession.sendMessage(
                new TextMessage(
                        new JSONObject()
                                .put("action", "auth")
                                .put("id", webSocketSession.getId())
                                .toString()
                )
        );

        sessionRepository.putSession(
                webSocketSession.getId(),
                new ArrayList<>(),
                webSocketSession
        );
    }

    @Override
    public void handleMessage(WebSocketSession webSocketSession, WebSocketMessage<?> webSocketMessage) throws Exception {

    }

    @Override
    public void handleTransportError(WebSocketSession webSocketSession, Throwable throwable) throws Exception {

    }

    @Override
    public void afterConnectionClosed(WebSocketSession webSocketSession, CloseStatus closeStatus) throws Exception {

    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
