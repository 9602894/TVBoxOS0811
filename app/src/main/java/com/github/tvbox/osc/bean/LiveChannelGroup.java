package com.github.tvbox.osc.bean;

import java.util.ArrayList;
import java.util.List;

public class LiveChannelGroup {
    private int groupIndex;
    private String groupName;
    private String groupPassword;
    private ArrayList liveChannelItems;

    public int getGroupIndex() {
        return groupIndex;
    }

    public void setGroupIndex(int groupIndex) {
        this.groupIndex = groupIndex;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public ArrayList getLiveChannels() {
        return liveChannelItems;
    }

    public void setLiveChannels(ArrayList liveChannelItems) {
        this.liveChannelItems = liveChannelItems;
    }

    // ========== 酷9兼容方法 ==========
    public void setLiveChannels(List<LiveChannelItem> liveChannelItems) {
        if (liveChannelItems instanceof ArrayList) {
            this.liveChannelItems = (ArrayList) liveChannelItems;
        } else {
            this.liveChannelItems = new ArrayList<>(liveChannelItems);
        }
    }
    // ========== 兼容方法结束 ==========

    public String getGroupPassword() {
        return groupPassword;
    }

    public void setGroupPassword(String groupPassword) {
        this.groupPassword = groupPassword;
    }
}

