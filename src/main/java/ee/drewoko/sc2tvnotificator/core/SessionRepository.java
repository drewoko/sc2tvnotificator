package ee.drewoko.sc2tvnotificator.core;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Deniss Gubanov on 15/03/15.
 * Project: sc2tvnotificator
 * Package: ee.drewoko.sc2tvnotificator.core
 */
@Component
public class SessionRepository {

    private Map<String, List<String>> tagList = new ConcurrentHashMap<>();
    private Map<String, WebSocketSession> sessionList = new ConcurrentHashMap<>();

    public void putSession(String sessionId, List<String> tags, WebSocketSession sessions) {
        sessionList.put(sessionId, sessions);
        tagList.put(sessionId, tags);
    }

    public void removeActiveSession(String sessionId) {
        sessionList.remove(sessionId);
        tagList.remove(sessionId);
    }

    public void setTagList(String sessionId, List<String> tags) {
        tagList.put(sessionId, tags);
    }

    public Map<String, List<String>> getTagList() {
        return tagList;
    }

    public WebSocketSession getWebSocketSession(String sessionId) {
        return sessionList.get(sessionId);
    }

}
