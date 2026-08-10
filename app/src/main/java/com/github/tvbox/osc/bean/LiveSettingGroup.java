package com.github.tvbox.osc.bean;

import java.util.ArrayList;
import java.util.List;

public class LiveSettingGroup {
    private int groupIndex;
    private String groupName;
    private ArrayList liveSettingItems;

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

    public ArrayList getLiveSettingItems() {
        return liveSettingItems;
    }

    public void setLiveSettingItems(ArrayList liveSettingItems) {
        this.liveSettingItems = liveSettingItems;
    }

    // ========== 酷9兼容方法 ==========
    public void setLiveSettingItems(List<LiveSettingItem> items) {
        if (items instanceof ArrayList) {
            this.liveSettingItems = (ArrayList) items;
        } else {
            this.liveSettingItems = new ArrayList<>(items);
        }
    }
    // ========== 兼容方法结束 ==========
}
