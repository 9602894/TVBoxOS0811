package com.github.tvbox.osc.util.epg;

import com.github.tvbox.osc.bean.Epginfo;
import com.github.tvbox.osc.bean.LiveChannelItem;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * EPG 多格式解析工具 (酷9特性)
 * 支持: DIYP, 百川, 超级TV, XMLTV, XML.GZ, JSON
 */
public class EpgUtil {

    public static final int TYPE_DIYP = 0;
    public static final int TYPE_BAICHUAN = 1;
    public static final int TYPE_CHAOJI = 2;
    public static final int TYPE_XMLTV = 3;
    public static final int TYPE_JSON = 4;

    /**
     * 根据类型解析EPG
     */
    public static List<Epginfo> parseEpg(String content, int type) {
        switch (type) {
            case TYPE_DIYP: return parseDiyp(content);
            case TYPE_BAICHUAN: return parseBaichuan(content);
            case TYPE_CHAOJI: return parseChaoji(content);
            case TYPE_XMLTV: return parseXmltv(content);
            case TYPE_JSON: return parseJson(content);
            default: return parseXmltv(content);
        }
    }

    /**
     * DIYP 格式解析
     */
    private static List<Epginfo> parseDiyp(String content) {
        List<Epginfo> list = new ArrayList<>();
        try {
            JSONObject json = new JSONObject(content);
            if (json.has("epg_data")) {
                JSONArray arr = json.getJSONArray("epg_data");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    Epginfo epg = new Epginfo();
                    epg.setTitle(obj.optString("title", ""));
                    epg.setStart(obj.optString("start", ""));
                    epg.setEnd(obj.optString("end", ""));
                    epg.setDesc(obj.optString("desc", ""));
                    list.add(epg);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    /**
     * 百川格式解析
     */
    private static List<Epginfo> parseBaichuan(String content) {
        List<Epginfo> list = new ArrayList<>();
        try {
            JSONObject json = new JSONObject(content);
            if (json.has("data")) {
                JSONObject data = json.getJSONObject("data");
                JSONArray arr = data.optJSONArray("epg");
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        Epginfo epg = new Epginfo();
                        epg.setTitle(obj.optString("title", ""));
                        epg.setStart(obj.optString("starttime", ""));
                        epg.setEnd(obj.optString("endtime", ""));
                        list.add(epg);
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    /**
     * 超级TV格式解析
     */
    private static List<Epginfo> parseChaoji(String content) {
        List<Epginfo> list = new ArrayList<>();
        try {
            JSONObject json = new JSONObject(content);
            JSONArray arr = json.optJSONArray("list");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    Epginfo epg = new Epginfo();
                    epg.setTitle(obj.optString("name", ""));
                    epg.setStart(obj.optString("start", ""));
                    epg.setEnd(obj.optString("end", ""));
                    list.add(epg);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    /**
     * XMLTV 格式解析
     */
    private static List<Epginfo> parseXmltv(String content) {
        List<Epginfo> list = new ArrayList<>();
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(content));

            int eventType = parser.getEventType();
            Epginfo currentEpg = null;

            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tagName = parser.getName();
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if ("programme".equals(tagName)) {
                            currentEpg = new Epginfo();
                            currentEpg.setStart(parser.getAttributeValue(null, "start"));
                            currentEpg.setEnd(parser.getAttributeValue(null, "stop"));
                        } else if ("title".equals(tagName) && currentEpg != null) {
                            currentEpg.setTitle(parser.nextText());
                        } else if ("desc".equals(tagName) && currentEpg != null) {
                            currentEpg.setDesc(parser.nextText());
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if ("programme".equals(tagName) && currentEpg != null) {
                            list.add(currentEpg);
                            currentEpg = null;
                        }
                        break;
                }
                eventType = parser.next();
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    /**
     * JSON 通用格式解析
     */
    private static List<Epginfo> parseJson(String content) {
        return parseDiyp(content); // 复用DIYP解析
    }

    /**
     * 构建EPG请求URL
     */
    public static String buildEpgUrl(String baseUrl, String channelName, String date, int type) {
        try {
            String encodedName = URLEncoder.encode(channelName, "UTF-8");
            switch (type) {
                case TYPE_DIYP:
                    return baseUrl.replace("{name}", encodedName).replace("{date}", date);
                case TYPE_BAICHUAN:
                    return baseUrl + "?ch=" + encodedName + "&date=" + date;
                case TYPE_CHAOJI:
                    return baseUrl + "?channel=" + encodedName + "&date=" + date;
                case TYPE_XMLTV:
                    return baseUrl.replace("{name}", encodedName).replace("{date}", date);
                default:
                    return baseUrl.replace("{name}", encodedName).replace("{date}", date);
            }
        } catch (Exception e) { return null; }
    }

    /**
     * 格式化EPG时间 (XMLTV格式: 20240101120000 +0000)
     */
    public static String formatEpgTime(String rawTime) {
        if (rawTime == null || rawTime.length() < 14) return rawTime;
        try {
            String year = rawTime.substring(0, 4);
            String month = rawTime.substring(4, 6);
            String day = rawTime.substring(6, 8);
            String hour = rawTime.substring(8, 10);
            String minute = rawTime.substring(10, 12);
            return hour + ":" + minute;
        } catch (Exception e) { return rawTime; }
    }
}
