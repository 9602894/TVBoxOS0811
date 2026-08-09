package com.github.tvbox.osc.util.live;

import com.github.tvbox.osc.bean.LiveChannelGroup;
import com.github.tvbox.osc.bean.LiveChannelItem;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * M3U/M3U8 直播源解析器 (酷9特性)
 * 支持: #EXTM3U, #EXTINF, tvg-id, tvg-name, tvg-logo, group-title, tvg-chno
 */
public class M3U8Subscribe {

    public static void parse(String url, LiveChannelGroup group) {
        OkGo.<String>get(url).execute(new AbsCallback<String>() {
            @Override public void onSuccess(Response<String> response) {
                parseContent(response.body(), group);
            }
            @Override public String convertResponse(okhttp3.Response response) throws Exception {
                return response.body().string();
            }
            @Override public void onError(Response<String> response) {
                super.onError(response);
            }
        });
    }

    public static void parseContent(String content, LiveChannelGroup group) {
        if (content == null || content.trim().isEmpty()) return;
        BufferedReader reader = new BufferedReader(new StringReader(content));
        String line;
        LiveChannelItem currentItem = null;
        int channelNum = 1;

        try {
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("#EXTINF:")) {
                    currentItem = new LiveChannelItem();
                    currentItem.setUrlList(new ArrayList<>());
                    currentItem.setChannelNum(channelNum++);

                    // 解析 tvg-name
                    Matcher nameMatcher = Pattern.compile("tvg-name="(.*?)"").matcher(line);
                    if (nameMatcher.find()) {
                        currentItem.setChannelName(nameMatcher.group(1));
                    }

                    // 解析 tvg-logo
                    Matcher logoMatcher = Pattern.compile("tvg-logo="(.*?)"").matcher(line);
                    if (logoMatcher.find()) {
                        currentItem.setChannelLogo(logoMatcher.group(1));
                    }

                    // 解析 group-title
                    Matcher groupMatcher = Pattern.compile("group-title="(.*?)"").matcher(line);
                    if (groupMatcher.find()) {
                        currentItem.setGroupName(groupMatcher.group(1));
                    }

                    // 解析 tvg-chno (频道号)
                    Matcher chnoMatcher = Pattern.compile("tvg-chno="(.*?)"").matcher(line);
                    if (chnoMatcher.find()) {
                        try {
                            currentItem.setChannelNum(Integer.parseInt(chnoMatcher.group(1)));
                        } catch (Exception ignored) {}
                    }

                    // 解析 tvg-id (EPG ID)
                    Matcher epgMatcher = Pattern.compile("tvg-id="(.*?)"").matcher(line);
                    if (epgMatcher.find()) {
                        currentItem.setEpg(epgMatcher.group(1));
                    }

                    // 如果没有 tvg-name，尝试从逗号后面获取
                    if (currentItem.getChannelName() == null || currentItem.getChannelName().isEmpty()) {
                        int commaIndex = line.lastIndexOf(",");
                        if (commaIndex > 0) {
                            currentItem.setChannelName(line.substring(commaIndex + 1).trim());
                        }
                    }
                } else if (!line.startsWith("#") && currentItem != null) {
                    // 这是URL行
                    currentItem.getUrlList().add(line);
                    if (currentItem.getChannelName() == null || currentItem.getChannelName().isEmpty()) {
                        currentItem.setChannelName("Channel " + currentItem.getChannelNum());
                    }
                    group.getLiveChannels().add(currentItem);
                    currentItem = null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
