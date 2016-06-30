package ee.drewoko.sc2tvnotificator.util;

import ee.drewoko.ApacheHttpWrapper.ApacheHttpWrapper;
import ee.drewoko.ApacheHttpWrapper.ApacheHttpWrapperMethod;
import ee.drewoko.ApacheHttpWrapper.ApacheHttpWrapperResponse;
import org.json.JSONObject;

import java.util.Map;

public class ChatHelper
{

    public static String getFunstreamUrl(String channel)
    {
        if (channel.equals("main"))
        {
            return "chat/main";
        }

        if(channel.startsWith("room"))
        {
            return getRoomName(channel);
        }

        return getChannelName(getChannelId(channel));
    }

    public static String getSc2TvUrl(Map<Integer, String> index, String channel)
    {
        if (channel.equals("main"))
        {
            return null;
        }

        if(channel.startsWith("room"))
        {
            return getRoomName(channel);
        }

        return index.get(getChannelId(channel));
    }

    public static String getRoomName(String channel)
    {
        int roomId = getChannelId(channel);

        ApacheHttpWrapper request = new ApacheHttpWrapper("http://funstream.tv/api/room", ApacheHttpWrapperMethod.POST);
        request.setRequestBody(new JSONObject().put("roomId", roomId).toString());

        ApacheHttpWrapperResponse response = request.exec();

        if(response.getHttpStatusCode() == 200)
        {
            return "room/" + response.getResponseJson().getString("slug");
        }

        return channel;
    }

    public static int getUserId(String name) {
        ApacheHttpWrapper request = new ApacheHttpWrapper("http://funstream.tv/api/user", ApacheHttpWrapperMethod.POST);
        request.setRequestBody(new JSONObject().put("name", name).toString());

        ApacheHttpWrapperResponse response = request.exec();
        if(response.getHttpStatusCode() != 200) {
            return 0;
        }
        return response.getResponseJson().getInt("id");
    }

    public static String getUserName(int id)
    {
        ApacheHttpWrapper request = new ApacheHttpWrapper("http://funstream.tv/api/user", ApacheHttpWrapperMethod.POST);
        request.setRequestBody(new JSONObject().put("id", id).toString());

        ApacheHttpWrapperResponse response = request.exec();
        if(response.getHttpStatusCode() != 200) {
            return null;
        }
        return response.getResponseJson().getString("name");
    }

    public static int getChannelId(String channel)
    {
        if (channel.equals("main"))
        {
            return 0;
        }
        String[] id = channel.split("/");
        return Integer.parseInt(id[1]);
    }

    public static String getChannelName(int channelId)
    {
        return "stream/" + getUserName(channelId);
    }

    public static String buildMessage(JSONObject message)
    {
        return ((message.get("to") instanceof JSONObject ? message.getJSONObject("to").getString("name") + ", " : "") + message.getString("text")).toLowerCase();
    }
}
