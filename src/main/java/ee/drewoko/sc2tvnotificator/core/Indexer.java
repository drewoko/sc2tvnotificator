package ee.drewoko.sc2tvnotificator.core;

import ee.drewoko.ApacheHttpWrapper.ApacheHttpWrapper;
import ee.drewoko.ApacheHttpWrapper.ApacheHttpWrapperResponse;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
/**
 * Created by Deniss Gubanov on 15/03/15.
 * Project: sc2tvnotificator
 * Package: ee.drewoko.sc2tvnotificator.indexer
 */

@Component
@EnableScheduling
public class Indexer {

    private static final Logger logger = Logger.getLogger(Indexer.class);

    private Map<Integer, String> index = new HashMap<>();

    @Scheduled(fixedRate = 60000)
    private void indexSc2tv() {
        logger.info("Indexing SC2TV");

        ApacheHttpWrapper request = new ApacheHttpWrapper("http://sc2tv.ru/streams_list.json");
        ApacheHttpWrapperResponse exec = request.exec();

        exec.getResponseJson().getJSONArray("streams").forEach(obj -> {
            JSONObject json = (JSONObject)obj;

            if(json.get("streamer_uid") != null && json.get("path") != null) {
                index.put(json.getInt("streamer_uid"), json.getString("path"));
            }
        });
    }

    public Map<Integer, String> getIndex()
    {
        return index;
    }
}

