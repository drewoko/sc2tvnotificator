package ee.drewoko.sc2tvnotificator.web;

import ee.drewoko.sc2tvnotificator.core.SessionRepository;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.util.ArrayList;

/**
 * Created by Deniss Gubanov on 15/03/15.
 * Project: sc2tvnotificator
 * Package: ee.drewoko.sc2tvnotificator.web
 */
public class ListenWebSocketHandler extends TextWebSocketHandler {

    @Resource
    SessionRepository sessionRepository;

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        try {

            JSONObject jsonMessage = new JSONObject(message.getPayload());

            if(jsonMessage.getString("action").equals("auth")) {

                session.sendMessage(
                        new TextMessage(
                                new JSONObject()
                                        .put("action", "auth")
                                        .put("id", session.getId())
                                        .toString()
                        )
                );

                sessionRepository.putSession(
                        session.getId(),
                        new ArrayList<>(),
                        session
                );

            }


        } catch (JSONException ignored) {

        }

    }







}
