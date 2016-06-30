package ee.drewoko.sc2tvnotificator.core;

import ee.drewoko.ApacheHttpWrapper.ApacheHttpWrapper;
import ee.drewoko.ApacheHttpWrapper.ApacheHttpWrapperMethod;
import ee.drewoko.ApacheHttpWrapper.ApacheHttpWrapperResponse;
import org.apache.log4j.Logger;
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
public class ChannelCollector
{

    private static final Logger logger = Logger.getLogger(ChannelCollector.class);

    private Map<Integer, String> index = new HashMap<>();

    private ChannelsStorage storage = ChannelsStorage.getInstance();

    @Scheduled(fixedRate = 60000)
    private void indexSc2tv() {
        logger.info("Indexing SC2TV");

//        ApacheHttpWrapper request = new ApacheHttpWrapper("http://sc2tv.ru/streams_list.json");
//        ApacheHttpWrapperResponse exec = request.exec();
//
//        exec.getResponseJson().getJSONArray("streams").forEach(obj -> {
//            JSONObject json = (JSONObject)obj;
//
//            if(json.get("streamer_uid") != null && json.get("path") != null) {
//                index.put(json.getInt("streamer_uid"), json.getString("path"));
//            }
//        });

        ApacheHttpWrapper request = new ApacheHttpWrapper("http://funstream.tv/api/content", ApacheHttpWrapperMethod.POST);
        request.setRequestBody(new JSONObject().put("content", "stream").put("type", "all").put("category", new JSONObject().put("slug", "top")).toString());

        ApacheHttpWrapperResponse exec = request.exec();
        if(exec.getHttpStatusCode() == 200)
        {
            exec.getResponseJson().getJSONArray("content").forEach(obj -> {
                JSONObject json = (JSONObject)obj;
                if(json.getJSONObject("owner") != null && json.get("slug") != null)
                {
                    int id = json.getJSONObject("owner").getInt("id");
                    index.put(id, "channel/" + json.getString("slug"));
                    storage.addChannel("stream/" + id);
                }
            });
        }
    }

    //old: "channel/zepp",
    //new: "zepp",

    public Map<Integer, String> getIndex()
    {
        return index;
    }
}

