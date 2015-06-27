package ee.drewoko.sc2tvnotificator.web;

import java.util.ArrayList;

/**
 * Created by Deniss Gubanov on 15/03/15.
 * Project: sc2tvnotificator
 * Package: ee.drewoko.sc2tvnotificator.web
 */
public class SetRequest {

    private String sessionId;
    private ArrayList<String> tags;

    public SetRequest() {
    }

    public SetRequest(String sessionId, ArrayList<String> tags) {
        this.sessionId = sessionId;
        this.tags = tags;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public ArrayList<String> getTags() {
        return tags;
    }

    public void setTags(ArrayList<String> tags) {
        this.tags = tags;
    }

    @Override
    public String toString() {
        return "SetRequest{" +
                "sessionId='" + sessionId + '\'' +
                ", tags=" + tags +
                '}';
    }
}
