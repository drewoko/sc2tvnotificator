package ee.drewoko.sc2tvnotificator.core;

import ee.drewoko.sc2tvnotificator.util.*;
import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
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

    private ChannelsStorage channelsStorage;

    @Resource
    private SessionRepository sessionRepository;

    @Resource
    private ChannelCollector channels;

    @PostConstruct
    public void init()
    {

        channelsStorage = ChannelsStorage.getInstance();

        try
        {
            IO.Options options = new IO.Options();
            options.reconnection = true;
            options.reconnectionDelay = 100;
            options.reconnectionAttempts = 9999;
            options.transports = new String[]{"websocket"};

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

        } catch (URISyntaxException e)
        {
            e.printStackTrace();
        }

    }

    private void connected(Object[] objects)
    {
        logger.info("Chat Connected");
        socket.emit("/chat/join", new JSONObject().put("channel", "all"));
    }

    public void readChat(Object... mes)
    {
        JSONObject currentMessage = (JSONObject) mes[0];

        Map<String, List<String>> tagList = sessionRepository.getTagList();

        channelsStorage.addChannel(currentMessage.getString("channel"));

        if (lastMessageId == 0)
        {
            lastMessageId = currentMessage.getInt("id");
        }

        if (tagList.size() > 0)
        {
            int currentMessageId = currentMessage.getInt("id");

            if (currentMessageId > lastMessageId)
            {
                lastMessageId = currentMessageId;
                String message = ChatHelper.buildMessage(currentMessage);
                for (Map.Entry<String, List<String>> entry : tagList.entrySet())
                {
                    String socketId = entry.getKey();
                    List<String> tags = entry.getValue();

                    tags.stream()
                            .filter(tag -> tagMatcher(currentMessage, message, tag, socketId))
                            .forEach(e -> sendNotification(e, socketId, currentMessage));

                }
            }
        }
    }

    private boolean tagMatcher(JSONObject currentMessage, String message, String tag, String sessionId)
    {
        return tag.startsWith(":u:") ?
            currentMessage.getJSONObject("from").getString("name").equalsIgnoreCase(tag.replace(":u:", "")) :
            message.contains(tag);
    }

    @Scheduled(fixedRate = 10000)
    private void userCheck()
    {
        Map<String, List<String>> tagList = sessionRepository.getTagList();
        for (Map.Entry<String, List<String>> entry : tagList.entrySet())
        {
            String socketId = entry.getKey();
            List<String> tags = entry.getValue();
            tags.stream()
                    .filter(tag -> tag.startsWith(":@:"))
                    .forEach(t -> checkUser(t.replace(":@:", ""), socketId));
        }
    }

    private void checkUser(String username, String sessionId)
    {
        int userId = ChatHelper.getUserId(username);
        channelsStorage.getChannels().forEach((channel) -> {
            socket.emit("/chat/channel/list", new JSONObject().put("channel", channel), (Ack) args -> {

                JSONObject json = (JSONObject) args[0];
                if (json.getString("status").equalsIgnoreCase("ok"))
                {
                    JSONArray result = json.getJSONObject("result").getJSONArray("users");
                    for (int i = 0; i < result.length(); i++)
                    {
                        if (userId == result.getInt(i))
                        {
                            sendSpyNotification(channel, username, sessionId);
                        }
                    }
                }

            });
        });
    }

    private void sendSpyNotification(String channel, String nickname, String sessionId)
    {
        try
        {
            //stream/name
            String urlFs = ChatHelper.getFunstreamUrl(channel);
            //channel/name
            String pathSc = ChatHelper.getSc2TvUrl(channels.getIndex(), channel);
            String urlSc = pathSc != null ? pathSc : urlFs;

            sessionRepository.getWebSocketSession(sessionId).sendMessage(
                    new TextMessage(
                            new JSONObject()
                                    .put("action", "mention")
                                    .put("data",
                                            new JSONObject()
                                                    .put("type", "spy")
                                                    .put("id", ChatHelper.getChannelId(channel))
                                                    .put("name", nickname)
                                                    .put("urlFs", urlFs)
                                                    .put("urlSc", urlSc)
                                    )
                                    .toString()
                    )
            );
        } catch (IOException | NullPointerException ignored)
        {
        } catch (IllegalStateException e)
        {
            sessionRepository.removeActiveSession(sessionId);
        }
    }

    private void sendNotification(String tag, String sessionId, JSONObject currentMessage)
    {
        //logger.info("Send Notification by tag: " + tag);
        try
        {
            //stream/name
            String urlFs = ChatHelper.getFunstreamUrl(currentMessage.getString("channel"));
            //channel/name
            String pathSc = ChatHelper.getSc2TvUrl(channels.getIndex(), currentMessage.getString("channel"));
            String urlSc = pathSc != null ? pathSc : urlFs;

            String nickname = currentMessage.get("to") instanceof JSONObject ? "[b]" + currentMessage.getJSONObject("to").getString("name") + "[/b], " : "";

            sessionRepository.getWebSocketSession(sessionId).sendMessage(
                    new TextMessage(
                            new JSONObject()
                                    .put("action", "mention")
                                    .put("data",
                                            new JSONObject()
                                                    .put("type", "def")
                                                    .put("id", currentMessage.getInt("id"))
                                                    .put("name", currentMessage.getJSONObject("from").getString("name"))
                                                    .put("message", nickname + currentMessage.getString("text"))
                                                    .put("urlFs", urlFs)
                                                    .put("urlSc", urlSc)
                                                    .put("date", currentMessage.getInt("time"))
                                    )
                                    .toString()
                    )
            );
        } catch (IOException | NullPointerException ignored)
        {
        } catch (IllegalStateException e)
        {
            sessionRepository.removeActiveSession(sessionId);
        }
    }
}
