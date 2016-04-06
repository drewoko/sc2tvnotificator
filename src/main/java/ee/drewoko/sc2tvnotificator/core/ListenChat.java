package ee.drewoko.sc2tvnotificator.core;

import ee.drewoko.sc2tvnotificator.core.util.PathHelper;
import io.socket.client.IO;
import io.socket.client.Socket;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

/**
 * Created by Deniss Gubanov on 15/03/15.
 * Project: sc2tvnotifier
 * Package: ee.drewoko.sc2tvnotifier.core
 */
@Component
@EnableScheduling
public class ListenChat {

    private static final Logger logger = Logger.getLogger(ListenChat.class);

    private int lastMessageId = 0;

    private Socket socket;

    @Resource
    SessionRepository sessionRepository;

    @Resource
    Indexer indexer;

    @PostConstruct
    public void init() {

        try {
            IO.Options options = new IO.Options();
            options.reconnection = true;
            options.reconnectionDelay = 100;
            options.reconnectionAttempts = 9999;

            options.transports = new String[] {"websocket"};

            socket = IO.socket("http://funstream.tv/", options);

            socket
                    .on(Socket.EVENT_CONNECT, this::connected)
                    .on(Socket.EVENT_DISCONNECT, args -> logger.info("WebSocket disconnected"))
                    .on(Socket.EVENT_ERROR, args -> logger.info("WebSocket connection error"))
                    .on(Socket.EVENT_RECONNECT, args -> logger.info("WebSocket reconnected"))
                    .on(Socket.EVENT_RECONNECT_ATTEMPT, args -> logger.info("WebSocket reconnected attempt"))
                    .on("/chat/message", this::readChat);

            logger.info("WebSocket connection attempt");
            socket.connect();

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }

    private void connected(Object[] objects) {
        logger.info("Chat Connected");
        join("all");
    }

    private void join(String channel) {
        try {
            socket.emit("/chat/join", new JSONObject().put("channel", channel));
            logger.info("Chat joined");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void readChat(Object... mes) {
        JSONObject currentMessage = (JSONObject) mes[0];
        try {
            if (lastMessageId == 0) {
                lastMessageId = currentMessage.getInt("id");
            } else {
                Map<String, List<String>> tagList = sessionRepository.getTagList();

                if(tagList.size() > 0) {
                        int currentMessageId = currentMessage.getInt("id");

                        if ( currentMessageId > lastMessageId ) {
                            lastMessageId = currentMessageId;

                            String message = ((currentMessage.get("to") instanceof JSONObject ? currentMessage.getJSONObject("to").getString("name") + ", " : "") + currentMessage.getString("text")).toLowerCase();
                            for (Map.Entry<String, List<String>> entry : tagList.entrySet()) {
                                String socketId = entry.getKey();
                                List<String> tags = entry.getValue();

                                tags.stream()
                                        .filter(tag -> tagMatcher(currentMessage, message, tag))
                                        .forEach(e -> sendNotification(e, socketId, currentMessage));

                            }
                        }
                    }
                }


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private boolean tagMatcher(JSONObject currentMessage, String message, String tag) {
        if (tag.startsWith(":")) {
            switch (tag.charAt(1)) {
                case 'u':
                    return currentMessage.getJSONObject("from").getString("name").equalsIgnoreCase(tag.replace(":u:", ""));
                case 'i':
                    if (message.contains(tag.toLowerCase().replace(":i:", "")))
                        return false; //ignore
                case 'w':
                    String search = tag.toLowerCase().replace(":w:", "");
                    if (message.matches(".*\\b(" + search.toLowerCase() + ")\\b.*"))
                        return false; //ignore
            }
        }
        return message.contains(tag);
    }

    private void sendNotification(String tag, String sessionId, JSONObject currentMessage)  {

        logger.info("Send Notification by tag: "+ tag);

        try {

            String funstreamTv = "http://funstream.tv/" + PathHelper.getFunstreamPath(currentMessage.getString("channel"));

            String sc2Path = PathHelper.getSc2TvPath(indexer.getIndex(), currentMessage.getString("channel"));
            String sc2tv = sc2Path == "main"? "http://funstream.tv/chat/main" : sc2Path == null ? "http://funstream.tv/" + PathHelper.getChannelLink(PathHelper.getIdFromChannel(currentMessage.getString("channel"))) : "http://sc2tv.ru/" + sc2Path;

            String nickname = currentMessage.get("to") instanceof JSONObject ? "[b]" + currentMessage.getJSONObject("to").getString("name") + "[/b], " : "";

                    sessionRepository.getWebSocketSession(sessionId).sendMessage(
                        new TextMessage(
                                new JSONObject()
                                        .put("action", "mention")
                                        .put("data",
                                                new JSONObject()
                                                        .put("id", currentMessage.getInt("id"))
                                                        .put("channelId", currentMessage.getString("channel"))
                                                        .put("name", currentMessage.getJSONObject("from").getString("name"))
                                                        .put("message", nickname + currentMessage.getString("text"))
                                                        .put("locationFS", funstreamTv)
                                                        .put("locationSC", sc2tv)
                                                        .put("date", currentMessage.getInt("time"))
                                        )
                                        .toString()
                        )
                );
        } catch (IOException | NullPointerException ignored) {
        } catch (IllegalStateException e) {
            sessionRepository.removeActiveSession(sessionId);
        }
    }
}
