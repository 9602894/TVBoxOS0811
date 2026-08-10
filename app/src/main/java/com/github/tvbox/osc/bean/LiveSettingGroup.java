package com.github.tvbox.osc.bean;

import java.util.ArrayList;
import java.util.List;

public class LiveSettingGroup {
    private int groupIndex;
    private String groupName;
    private List<LiveSettingItem> liveSettingItems;

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

    public List<LiveSettingItem> getLiveSettingItems() {
        return liveSettingItems;
    }

    public void setLiveSettingItems(List<LiveSettingItem> liveSettingItems) {
        this.liveSettingItems = liveSettingItems;
    }
}
