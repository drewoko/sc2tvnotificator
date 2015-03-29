package ee.drewoko.sc2tvnotificator.core;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

    private static final String USERAGENT = "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.0.1) Gecko/2008070208 Firefox/3.0.1";

    private int lastMessageId = 0;

    @Resource
    SessionRepository sessionRepository;

    @Resource
    Indexer indexer;

    @Scheduled(fixedRate = 3000)
    public void readChat() {

        logger.info("Reading SC2TV.ru chat");

        CloseableHttpClient httpClient = HttpClients.createDefault();

        HttpGet httpGet = new HttpGet("http://chat.sc2tv.ru/memfs/channel-moderator.json");
        httpGet.addHeader("User-Agent", USERAGENT);

        try {
            HttpResponse response = httpClient.execute(httpGet);

            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            StringBuilder result = new StringBuilder();
            String line;

            while ((line = rd.readLine()) != null)
                result.append(line);

            JSONArray messageJson = new JSONObject(result.toString()).getJSONArray("messages");

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

                                tags.stream().filter(message::contains).forEach(e -> sendNotification(e, socketId, currentMessage));

                            }
                        }

                    }
                }

            }


        } catch (IOException ignored) {
        }

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
