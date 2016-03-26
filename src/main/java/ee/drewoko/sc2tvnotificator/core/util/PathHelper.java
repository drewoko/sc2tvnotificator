package ee.drewoko.sc2tvnotificator.core.util;

import ee.drewoko.ApacheHttpWrapper.ApacheHttpWrapper;
import ee.drewoko.ApacheHttpWrapper.ApacheHttpWrapperMethod;
import ee.drewoko.ApacheHttpWrapper.ApacheHttpWrapperResponse;
import org.json.JSONObject;

import java.util.Map;

public class PathHelper {

    public static String getFunstreamPath(String channel)
    {
        if (channel.equals("main"))
        {
            return "chat/main";
        }

        if(channel.startsWith("room"))
        {
            return getRoomName(channel);
        }

        int channelId = getIdFromChannel(channel);

        ApacheHttpWrapper request = new ApacheHttpWrapper("http://funstream.tv/api/user", ApacheHttpWrapperMethod.POST);
        request.setRequestBody(new JSONObject().put("id", channelId).toString());

        ApacheHttpWrapperResponse response = request.exec();

        return "stream/" + response.getResponseJson().getString("name");
    }

    public static String getSc2TvPath(Map<Integer, String> index, String channel)
    {
        if (channel.equals("main"))
        {
            return "main";
        }

        if(channel.startsWith("room"))
        {
            return getRoomName(channel);
        }

        return index.get(getIdFromChannel(channel));
    }

    private static String getRoomName(String channel)
    {
        int roomId = getIdFromChannel(channel);

        ApacheHttpWrapper request = new ApacheHttpWrapper("http://funstream.tv/api/room", ApacheHttpWrapperMethod.POST);
        request.setRequestBody(new JSONObject().put("roomId", roomId).toString());

        ApacheHttpWrapperResponse response = request.exec();

        if(response.getHttpStatusCode()!= 200)
        {
            return channel;
        }

        return "room/" + response.getResponseJson().getString("slug");
    }

    private static int getIdFromChannel(String channel)
    {
        String[] id = channel.split("/");
        return Integer.parseInt(id[1]);
    }
}
