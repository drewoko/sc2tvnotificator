package ee.drewoko.sc2tvnotificator.core;

import java.nio.channels.Channel;
import java.util.ArrayList;
import java.util.List;

public class ChannelsStorage
{
    private static ChannelsStorage instance;
    private List<String> channels = new ArrayList<>();

    private ChannelsStorage() {}

    public static synchronized ChannelsStorage getInstance() {
        if (instance == null) {
            instance = new ChannelsStorage();
            instance.addChannel("main");
        }
        return instance;
    }

    public void addChannel(String channel) {
        if(channels.contains(channel)) {
            return;
        }
        channels.add(channel);
    }

    public List<String> getChannels() {
        return channels;
    }
}
