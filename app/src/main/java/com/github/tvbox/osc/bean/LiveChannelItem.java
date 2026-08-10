package com.github.tvbox.osc.bean;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LiveChannelItem {
    private int channelIndex;
    private int channelNum;
    private String channelName;
    private String channelLogo;
    private String channelEpg;
    private String channelUa;
    private String channelClick;
    private String channelFormat;
    private String channelOrigin;
    private String channelReferer;
    private String channelTvgId;
    private String channelTvgName;
    private JsonObject channelCatchup;
    private Map channelHeader;
    private Integer channelParse;
    private ArrayList channelSourceNames;
    private ArrayList channelUrls;
    public int sourceIndex = 0;
    public int sourceNum = 0;
    public boolean include_back = false;

    // 酷9兼容字段
    private String groupName;
    private String epg;

    public void setinclude_back(boolean include_back) {
        this.include_back = include_back;
    }

    public boolean getinclude_back() {
        return include_back;
    }

    public void setChannelIndex(int channelIndex) {
        this.channelIndex = channelIndex;
    }

    public int getChannelIndex() {
        return channelIndex;
    }

    public void setChannelNum(int channelNum) {
        this.channelNum = channelNum;
    }

    public int getChannelNum() {
        return channelNum;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelLogo(String channelLogo) {
        this.channelLogo = channelLogo;
    }

    public String getChannelLogo() {
        return channelLogo == null ? "" : channelLogo;
    }

    public void setChannelEpg(String channelEpg) {
        this.channelEpg = channelEpg;
    }

    public String getChannelEpg() {
        return channelEpg == null ? "" : channelEpg;
    }

    public void setChannelUa(String channelUa) {
        this.channelUa = channelUa;
    }

    public String getChannelUa() {
        return channelUa == null ? "" : channelUa;
    }

    public void setChannelClick(String channelClick) {
        this.channelClick = channelClick;
    }

    public String getChannelClick() {
        return channelClick == null ? "" : channelClick;
    }

    public void setChannelFormat(String channelFormat) {
        this.channelFormat = channelFormat;
    }

    public String getChannelFormat() {
        return channelFormat == null ? "" : channelFormat;
    }

    public void setChannelOrigin(String channelOrigin) {
        this.channelOrigin = channelOrigin;
    }

    public String getChannelOrigin() {
        return channelOrigin == null ? "" : channelOrigin;
    }

    public void setChannelReferer(String channelReferer) {
        this.channelReferer = channelReferer;
    }

    public String getChannelReferer() {
        return channelReferer == null ? "" : channelReferer;
    }

    public void setChannelTvgId(String channelTvgId) {
        this.channelTvgId = channelTvgId;
    }

    public String getChannelTvgId() {
        return channelTvgId == null ? "" : channelTvgId;
    }

    public void setChannelTvgName(String channelTvgName) {
        this.channelTvgName = channelTvgName;
    }

    public String getChannelTvgName() {
        return channelTvgName == null ? "" : channelTvgName;
    }

    public void setChannelCatchup(JsonObject channelCatchup) {
        this.channelCatchup = channelCatchup;
    }

    public JsonObject getChannelCatchup() {
        return channelCatchup == null ? new JsonObject() : channelCatchup;
    }

    public boolean hasCatchup() {
        return channelCatchup != null && channelCatchup.entrySet().size() > 0;
    }

    public void setChannelHeader(Map channelHeader) {
        this.channelHeader = channelHeader;
    }

    public Map getChannelHeader() {
        return channelHeader == null ? new HashMap() : channelHeader;
    }

    public void setChannelParse(Integer channelParse) {
        this.channelParse = channelParse;
    }

    public int getChannelParse() {
        return channelParse == null ? 0 : channelParse.intValue();
    }

    public Map getHeaders() {
        Map headers = new HashMap<>(getChannelHeader());
        if (!getChannelUa().isEmpty()) headers.put("User-Agent", getChannelUa());
        if (!getChannelOrigin().isEmpty()) headers.put("Origin", getChannelOrigin());
        if (!getChannelReferer().isEmpty()) headers.put("Referer", getChannelReferer());
        return headers;
    }

    public ArrayList getChannelUrls() {
        return channelUrls;
    }

    public void setChannelUrls(ArrayList channelUrls) {
        this.channelUrls = channelUrls;
        sourceNum = channelUrls == null ? 0 : channelUrls.size();
    }

    // ========== 酷9兼容方法 ==========
    public List<String> getUrlList() {
        if (channelUrls == null) return new ArrayList<>();
        return new ArrayList<>(channelUrls);
    }

    public void setUrlList(List<String> urls) {
        if (urls instanceof ArrayList) {
            this.channelUrls = (ArrayList) urls;
        } else {
            this.channelUrls = new ArrayList<>(urls);
        }
        sourceNum = channelUrls == null ? 0 : channelUrls.size();
    }

    public String getEpg() {
        return epg != null ? epg : getChannelEpg();
    }

    public void setEpg(String epg) {
        this.epg = epg;
        this.channelEpg = epg;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    // ========== 兼容方法结束 ==========

    public void preSource() {
        sourceIndex--;
        if (sourceIndex < 0) sourceIndex = sourceNum - 1;
    }

    public void nextSource() {
        sourceIndex++;
        if (sourceIndex == sourceNum) sourceIndex = 0;
    }

    public void setSourceIndex(int sourceIndex) {
        this.sourceIndex = sourceIndex;
    }

    public int getSourceIndex() {
        return sourceIndex;
    }

    public String getUrl() {
        if (channelUrls == null || channelUrls.isEmpty()) return "";
        return (String) channelUrls.get(sourceIndex);
    }

    public int getSourceNum() {
        return sourceNum;
    }

    public ArrayList getChannelSourceNames() {
        return channelSourceNames;
    }

    public void setChannelSourceNames(ArrayList channelSourceNames) {
        this.channelSourceNames = channelSourceNames;
    }

    public String getSourceName() {
        if (channelSourceNames == null || channelSourceNames.isEmpty()) return "";
        return (String) channelSourceNames.get(sourceIndex);
    }

    public boolean isEmptyCatchup() {
        return channelCatchup == null || channelCatchup.entrySet().size() == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LiveChannelItem that = (LiveChannelItem) o;
        return Objects.equals(channelName, that.channelName)
                && Objects.equals(getUrl(), that.getUrl());
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelName, getUrl());
    }
}
