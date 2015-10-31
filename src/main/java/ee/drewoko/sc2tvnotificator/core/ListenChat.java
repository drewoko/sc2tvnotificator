package ee.drewoko.sc2tvnotificator.core;

import ee.drewoko.ApacheHttpWrapper.ApacheHttpWrapper;
import ee.drewoko.ApacheHttpWrapper.ApacheHttpWrapperResponse;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

import javax.annotation.Resource;
import java.io.IOException;
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

    @Resource
    SessionRepository sessionRepository;

    @Resource
    Indexer indexer;

    @Scheduled(fixedRate = 3000)
    public void readChat() {

        logger.info("Reading SC2TV.ru chat");

        ApacheHttpWrapper httpRequest = new ApacheHttpWrapper("http://chat.sc2tv.ru/memfs/channel-moderator.json");
        ApacheHttpWrapperResponse response = httpRequest.exec();

        JSONArray messageJson = response.getResponseJson().getJSONArray("messages");

        if(lastMessageId == 0)
            lastMessageId = Integer.parseInt(
                    messageJson.getJSONObject(0).getString("id")
            );
        else {

            Map<String, List<String>> tagList = sessionRepository.getTagList();

            if(tagList.size() > 0) {
                for (int i = (messageJson.length() - 1); i >= 0; i--) {

                    JSONObject currentMessage = messageJson.getJSONObject(i);

                    int currentMessageId = Integer.parseInt(currentMessage.getString("id"));

                    if ( currentMessageId > lastMessageId ) {

                        lastMessageId = currentMessageId;

                        String message = currentMessage.getString("message").toLowerCase();

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

        }

    }

    private boolean tagMatcher(JSONObject currentMessage, String message, String tag) {

        return tag.startsWith(":u:") ?
                currentMessage.getString("name").equalsIgnoreCase(tag.replace(":u:", "")) :
                message.contains(tag);
    }

    private void sendNotification(String tag, String sessionId, JSONObject currentMessage)  {

        logger.info("Send Notification by tag: "+ tag);

        try {

            String path = indexer.getIndexedPath(Integer.parseInt(currentMessage.getString("channelId")));

             String site = path == null ?
             "http://chat.sc2tv.ru/index.htm?channelId=" + currentMessage.getString("channelId") :
             "http://sc2tv.ru/"+path;

                sessionRepository.getWebSocketSession(sessionId).sendMessage(
                        new TextMessage(
                                new JSONObject()
                                        .put("action", "mention")
                                        .put("data",
                                                new JSONObject()
                                                        .put("id", currentMessage.getInt("id"))
                                                        .put("channelId", currentMessage.getInt("channelId"))
                                                        .put("name", currentMessage.getString("name"))
                                                        .put("message", currentMessage.getString("message"))
                                                        .put("location", site)
                                                        .put("date", currentMessage.getString("date"))
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
