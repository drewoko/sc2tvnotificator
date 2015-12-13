package ee.drewoko.sc2tvnotificator.core;

import ee.drewoko.ApacheHttpWrapper.ApacheHttpWrapper;
import ee.drewoko.ApacheHttpWrapper.ApacheHttpWrapperMethod;
import ee.drewoko.ApacheHttpWrapper.ApacheHttpWrapperResponse;
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

                            String message = currentMessage.getString("text").toLowerCase();
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

        return tag.startsWith(":u:") ?
                currentMessage.getJSONObject("from").getString("name").equalsIgnoreCase(tag.replace(":u:", "")) :
                message.contains(tag);
    }

    private void sendNotification(String tag, String sessionId, JSONObject currentMessage)  {

        logger.info("Send Notification by tag: "+ tag);

        try {

            String funstreamPath = getStreamPath(getIdFromChannel(currentMessage.getString("channel")));
            String sc2Path = indexer.getSc2TvPath(getIdFromChannel(currentMessage.getString("channel")));

            String siteFunstreamtv = funstreamPath == null ?
                    "https://funstream.tv/chat/main" :
                    "https://funstream.tv/stream/" + funstreamPath;

            String siteSc2Tv = sc2Path == null ?
                    "http://chat.sc2tv.ru/index.htm?channelId=" + getIdFromChannel(currentMessage.getString("channel")) :
                    "http://sc2tv.ru/" + sc2Path;

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
                                                        .put("message", nickname +
                                                                        currentMessage.getString("text"))
                                                        .put("locationFS", siteFunstreamtv)
                                                        .put("locationSC", siteSc2Tv)
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

    public String getStreamPath(int channelId) {
        if (channelId == 0)
            return null;
        ApacheHttpWrapper request = new ApacheHttpWrapper("https://funstream.tv/api/user", ApacheHttpWrapperMethod.POST);
        request.setRequestBody(new JSONObject().put("id", channelId).toString());

        ApacheHttpWrapperResponse response = request.exec();

        return response.getResponseJson().getString("name");
    }

    private int getIdFromChannel(String channel) {
        if(channel.equals("main"))
            return 0;
        String[] id = channel.split("/");
        return Integer.parseInt(id[1]);
    }
}
