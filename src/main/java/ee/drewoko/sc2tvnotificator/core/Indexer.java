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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

    private static final String USERAGENT = "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.0.1) Gecko/2008070208 Firefox/3.0.1";


    @Scheduled(fixedRate = 30000)
    private void indexSc2tv() {
        logger.info("Indexing SC2TV2");

        CloseableHttpClient httpClient = HttpClients.createDefault();

        HttpGet httpGet = new HttpGet("http://sc2tv.ru/streams_list.json");
        httpGet.addHeader("User-Agent", USERAGENT);

        HttpResponse response = null;

        try {
            response = httpClient.execute(httpGet);

            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            StringBuilder result = new StringBuilder();
            String line;

            while ((line = rd.readLine()) != null)
                result.append(line);

            JSONArray streamsJson = new JSONObject(result.toString()).getJSONArray("streams");

                for (int i = (streamsJson.length() - 1); i >= 0; i--) {

                    JSONObject currentStream = streamsJson.getJSONObject(i);

                    index.put(currentStream.getInt("id"), currentStream.getString("path"));

                }


        } catch (IOException ignored) {
        }

    }

    public String getIndexedPath(Integer id) {
        return index.get(id);
    }


}
