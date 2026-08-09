package com.github.tvbox.osc.util.backup;

import android.content.Context;
import android.os.Environment;

import com.github.tvbox.osc.util.HawkConfig;
import com.orhanobut.hawk.Hawk;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

/**
 * 备份恢复工具 (酷9特性)
 */
public class BackupUtil {

    private static final String BACKUP_DIR = "TVBoxBackup";

    public static boolean exportConfig(Context context) {
        try {
            JSONObject config = new JSONObject();
            config.put("api_url", Hawk.get(HawkConfig.API_URL, ""));
            config.put("home_source", Hawk.get(HawkConfig.HOME_API, ""));
            config.put("live_player_type", Hawk.get(HawkConfig.LIVE_PLAYER_TYPE, 0));
            config.put("parse_webview", Hawk.get(HawkConfig.PARSE_WEBVIEW, true));
            config.put("show_preview", Hawk.get(HawkConfig.SHOW_PREVIEW, true));
            config.put("search_type", Hawk.get(HawkConfig.SEARCH_VIEW, 0));

            // 收藏列表
            // config.put("collect", ...);

            String fileName = "tvbox_backup_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".json";
            File dir = new File(Environment.getExternalStorageDirectory(), BACKUP_DIR);
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, fileName);

            FileWriter writer = new FileWriter(file);
            writer.write(config.toString(2));
            writer.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean importConfig(Context context, File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            JSONObject config = new JSONObject(sb.toString());
            if (config.has("api_url")) Hawk.put(HawkConfig.API_URL, config.getString("api_url"));
            if (config.has("home_source")) Hawk.put(HawkConfig.HOME_API, config.getString("home_source"));
            if (config.has("live_player_type")) Hawk.put(HawkConfig.LIVE_PLAYER_TYPE, config.getInt("live_player_type"));

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
