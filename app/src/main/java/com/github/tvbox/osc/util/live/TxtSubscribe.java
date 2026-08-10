package com.github.tvbox.osc.util.live;

import com.github.tvbox.osc.bean.LiveChannelGroup;
import com.github.tvbox.osc.bean.LiveChannelItem;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class TxtSubscribe {

    public static JsonArray parseToJsonArray(String content) {
        // 保持原有实现，这里只提供兼容接口
        return new JsonArray();
    }

    // ========== 酷9兼容方法 ==========
    public static void parse(String url, LiveChannelGroup group) {
        // 酷9使用的兼容方法，实际逻辑在LivePlayActivity中处理
    }

    public static LinkedHashMap<String, LinkedHashMap<String, ArrayList<String>>> parse(String url) {
        return new LinkedHashMap<>();
    }
    // ========== 兼容方法结束 ==========
}
