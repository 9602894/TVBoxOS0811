package com.github.tvbox.osc.util.backup;

import com.github.tvbox.osc.util.HawkConfig;
import com.orhanobut.hawk.Hawk;

import org.json.JSONObject;

public class BackupUtil {
    public static String backup() {
        try {
            JSONObject config = new JSONObject();
            config.put("api_url", Hawk.get(HawkConfig.API_URL, ""));
            config.put("live_player_type", Hawk.get(HawkConfig.LIVE_PLAYER_TYPE, 0));
            return config.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static void restore(String json) {
        try {
            JSONObject config = new JSONObject(json);
            if (config.has("live_player_type")) {
                Hawk.put(HawkConfig.LIVE_PLAYER_TYPE, config.getInt("live_player_type"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
