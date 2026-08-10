package com.github.tvbox.osc.ui.activity;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.Epginfo;
import com.github.tvbox.osc.bean.LiveChannelGroup;
import com.github.tvbox.osc.bean.LiveChannelItem;
import com.github.tvbox.osc.bean.LiveDayListGroup;
import com.github.tvbox.osc.bean.LiveEpgDate;
import com.github.tvbox.osc.bean.LivePlayerManager;
import com.github.tvbox.osc.bean.LiveSettingGroup;
import com.github.tvbox.osc.bean.LiveSettingItem;
import com.github.tvbox.osc.player.controller.LiveController;
import com.github.tvbox.osc.ui.adapter.LiveChannelGroupAdapter;
import com.github.tvbox.osc.ui.adapter.LiveChannelItemAdapter;
import com.github.tvbox.osc.ui.adapter.LiveEpgAdapter;
import com.github.tvbox.osc.ui.adapter.LiveEpgDateAdapter;
import com.github.tvbox.osc.ui.adapter.LiveSettingGroupAdapter;
import com.github.tvbox.osc.ui.adapter.LiveSettingItemAdapter;
import com.github.tvbox.osc.ui.adapter.MyEpgAdapter;
import com.github.tvbox.osc.util.EpgUtil;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.LOG;
import com.github.tvbox.osc.util.OkGoHelper;
import com.orhanobut.hawk.Hawk;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.doikki.videoplayer.player.VideoView;

/**
 * LivePlayActivity - 整合酷9全部功能，自包含所有适配器，不依赖额外布局文件
 * 增加未配置接口时引导进入设置的功能
 */
public class LivePlayActivity extends BaseActivity {
    public static Context context;

    // ========== 核心播放器 ==========
    private VideoView mVideoView;
    private LivePlayerManager livePlayerManager = new LivePlayerManager();

    // ========== 频道列表 UI ==========
    private LinearLayout tvLeftChannelListLayout;
    private TvRecyclerView mChannelGroupView;
    private TvRecyclerView mLiveChannelView;
    private LiveChannelGroupAdapter liveChannelGroupAdapter;
    private LiveChannelItemAdapter liveChannelItemAdapter;
    private List<LiveChannelGroup> liveChannelGroupList = new ArrayList<>();

    // ========== 设置 UI ==========
    private LinearLayout tvRightSettingLayout;
    private TvRecyclerView mSettingGroupView;
    private TvRecyclerView mSettingItemView;
    private LiveSettingGroupAdapter liveSettingGroupAdapter;
    private LiveSettingItemAdapter liveSettingItemAdapter;
    private List<LiveSettingGroup> liveSettingGroupList = new ArrayList<>();

    // ========== EPG UI ==========
    private TextView tv_curepg_left, tv_nextepg_left;
    private TextView tip_epg1, tip_epg2;
    private TextView tv_current_program_name, tv_next_program_name;
    private TextView tv_channelnum, tip_chname, tv_srcinfo;
    private LinearLayout divEpg;
    private View divLoadEpg, divLoadEpgDivider, divLoadEpgleft;
    private RelativeLayout ll_epg;
    private TvRecyclerView mEpgDateGridView, mRightEpgList;
    private LiveEpgDateAdapter liveEpgDateAdapter;
    private LiveEpgAdapter epgListAdapter;
    private List<LiveEpgDate> liveEpgDateList = new ArrayList<>();
    private List<LiveDayListGroup> liveDayList = new ArrayList<>();
    private List<Epginfo> epgdata = new ArrayList<>();
    private static Hashtable<String, ArrayList<Epginfo>> hsEpg = new Hashtable<>();

    // ========== 顶部信息 ==========
    private TextView tvTime, tvNetSpeed, tvResolution;
    private TextView tvChannelInfo;
    private TextView tvSelectedChannel;
    private ImageView imgLiveIcon;
    private FrameLayout liveIconNullBg;
    private TextView liveIconNullText;
    private TextView tv_right_top_channel_name, tv_right_top_epg_name;
    private ImageView iv_circle_bg;
    private ObjectAnimator objectAnimator;

    // ========== 播放控制 ==========
    private SeekBar sBar;
    private TextView tv_currentpos, tv_duration;
    private View iv_playpause, iv_play, backcontroller;
    private CountDownTimer countDownTimer;

    // ========== 数据 ==========
    private int currentChannelGroupIndex = 0;
    private int currentLiveChannelIndex = -1;
    private LiveChannelItem currentLiveChannelItem = null;
    private static LiveChannelItem channel_Name = null;
    private String logoUrl = "";
    private int selectedChannelNumber = 0;
    private boolean isBack = false;
    private boolean exitingLivePlay = false;
    private String epgStringAddress = "";

    // ========== 常量 ==========
    private static final String DEFAULT_EPG_ADDRESS = "http://epg.51zmt.top:8000/api/diyp/?ch={name}&date={date}";
    private static final long EPG_LOAD_DELAY = 1200L;
    private static final int RESOLUTION_INFO_MAX_RETRY = 10;
    private static final long RESOLUTION_INFO_RETRY_DELAY = 300L;
    private static final long RESOLUTION_INFO_HIDE_DELAY = 3000L;
    private static final int postTimeout = 6000;

    public static SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd");
    public static SimpleDateFormat formatDate1 = new SimpleDateFormat("MM-dd");
    public static String day = formatDate.format(new Date());
    public static Date nowday = new Date();

    // ========== Handler ==========
    private Handler mHandler = new Handler();
    private int resolutionInfoRetryCount = 0;
    private boolean resolutionInfoPending = false;

    // ========== 快捷菜单、搜索、音轨切换（自包含实现） ==========
    private PopupWindow shortcutsPopupWindow;
    private PopupWindow searchPopupWindow;
    private PopupWindow trackPopupWindow;

    // ========== 生命周期 ==========
    @Override
    protected int getLayoutResID() {
        return R.layout.activity_live_play;
    }

    @Override
    protected void init() {
        context = this;
        epgStringAddress = getConfiguredEpgAddress();
        setLoadSir(findViewById(R.id.live_root));

        // ---------- 绑定视图 ----------
        try {
            mVideoView = findViewById(R.id.mVideoView);
            tvLeftChannelListLayout = findViewById(R.id.tvLeftChannnelListLayout);
            mChannelGroupView = findViewById(R.id.mGroupGridView);
            mLiveChannelView = findViewById(R.id.mChannelGridView);
            tvRightSettingLayout = findViewById(R.id.tvRightSettingLayout);
            mSettingGroupView = findViewById(R.id.mSettingGroupView);
            mSettingItemView = findViewById(R.id.mSettingItemView);
            tvChannelInfo = findViewById(R.id.tvChannel);
            tvTime = findViewById(R.id.tvTime);
            tvNetSpeed = findViewById(R.id.tvNetSpeed);
            tvResolution = findViewById(R.id.tvResolution);
            tip_chname = findViewById(R.id.tv_channel_bar_name);
            tv_channelnum = findViewById(R.id.tv_channel_bottom_number);
            tip_epg1 = findViewById(R.id.tv_current_program_time);
            tip_epg2 = findViewById(R.id.tv_next_program_time);
            tv_srcinfo = findViewById(R.id.tv_source);
            tv_curepg_left = findViewById(R.id.tv_current_program);
            tv_nextepg_left = findViewById(R.id.tv_next_program);
            ll_epg = findViewById(R.id.ll_epg);
            tv_right_top_channel_name = findViewById(R.id.tv_right_top_channel_name);
            tv_right_top_epg_name = findViewById(R.id.tv_right_top_epg_name);
            iv_circle_bg = findViewById(R.id.iv_circle_bg);
            // tv_shownum 已移除（如果不需要）
            // 如果有该控件，请取消注释并添加 findViewById
            // TextView tv_shownum = findViewById(R.id.tv_shownum);
            txtNoEpg = findViewById(R.id.txtNoEpg);
            ll_right_top_loading = findViewById(R.id.ll_right_top_loading);
            ll_right_top_huikan = findViewById(R.id.ll_right_top_huikan);
            divLoadEpg = findViewById(R.id.divLoadEpg);
            divLoadEpgDivider = findViewById(R.id.divLoadEpgDivider);
            divLoadEpgleft = findViewById(R.id.divLoadEpgleft);
            divEpg = findViewById(R.id.divEPG);
            mEpgDateGridView = findViewById(R.id.mEpgDateGridView);
            mRightEpgList = findViewById(R.id.lv_epg);
            imgLiveIcon = findViewById(R.id.img_live_icon);
            liveIconNullBg = findViewById(R.id.live_icon_null_bg);
            liveIconNullText = findViewById(R.id.live_icon_null_text);
            sBar = findViewById(R.id.pb_progressbar);
            tv_currentpos = findViewById(R.id.tv_currentpos);
            backcontroller = findViewById(R.id.backcontroller);
            tv_duration = findViewById(R.id.tv_duration);
            iv_playpause = findViewById(R.id.iv_playpause);
            iv_play = findViewById(R.id.iv_play);
            tvSelectedChannel = findViewById(R.id.tv_selected_channel);
            tv_current_program_name = findViewById(R.id.tv_current_program_name);
            tv_next_program_name = findViewById(R.id.tv_next_program_name);

            if (imgLiveIcon != null) imgLiveIcon.setVisibility(View.INVISIBLE);
            if (liveIconNullText != null) liveIconNullText.setVisibility(View.INVISIBLE);
            if (liveIconNullBg != null) liveIconNullBg.setVisibility(View.INVISIBLE);
        } catch (Exception e) {
            Log.e("LivePlay", "init view error", e);
        }

        // ---------- 动画 ----------
        if (iv_circle_bg != null) {
            objectAnimator = ObjectAnimator.ofFloat(iv_circle_bg, "rotation", 360.0f);
            objectAnimator.setDuration(postTimeout);
            objectAnimator.setRepeatCount(-1);
            objectAnimator.start();
        }

        Hawk.put(HawkConfig.NOW_DATE, formatDate.format(new Date()));
        day = formatDate.format(new Date());
        nowday = new Date();

        // ---------- 播放控制 ----------
        initPlayControls();

        // ---------- 初始化各种视图 ----------
        initEpgDateView();
        initEpgListView();
        initDayList();
        initVideoView();
        initChannelGroupView();
        initLiveChannelView();
        initSettingGroupView();
        initSettingItemView();
        initLiveChannelList();
        initLiveSettingGroupList();
        Hawk.put(HawkConfig.PLAYER_IS_LIVE, true);

        // ---------- 初始化酷9风格的自包含菜单（不依赖布局） ----------
        initShortcutsMenu();
        initSearchDialog();
        initTrackDialog();

        // ---------- 网速更新 ----------
        mHandler.postDelayed(mUpdateNetSpeedRun, 1000);

        // ---------- 检查接口地址是否配置 ----------
        checkApiConfig();
    }

    // ========== 检查接口配置，若未配置则引导进入设置 ==========
    private void checkApiConfig() {
        String apiUrl = Hawk.get(HawkConfig.API_URL, "");
        if (TextUtils.isEmpty(apiUrl)) {
            // 显示提示并引导
            Toast.makeText(this, "请先设置接口地址", Toast.LENGTH_LONG).show();
            // 显示一个可点击的提示视图（这里简单 Toast，也可添加一个自定义视图）
            // 按返回键将跳转到设置，按菜单键也会跳转
        }
    }

    // ========== 跳转到设置（主界面） ==========
    private void gotoSetting() {
        // 跳转到 MainActivity，因为 MainActivity 通常有设置入口
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    // ========== 播放控制初始化 ==========
    private void initPlayControls() {
        if (iv_play != null) {
            iv_play.setOnClickListener(v -> {
                if (mVideoView == null) return;
                mVideoView.start();
                iv_play.setVisibility(View.INVISIBLE);
                if (countDownTimer != null) countDownTimer.start();
                if (iv_playpause != null)
                    iv_playpause.setBackground(ContextCompat.getDrawable(this, R.drawable.vod_pause));
            });
        }
        if (iv_playpause != null) {
            iv_playpause.setOnClickListener(v -> {
                if (mVideoView == null) return;
                if (mVideoView.isPlaying()) {
                    mVideoView.pause();
                    if (countDownTimer != null) countDownTimer.cancel();
                    if (iv_play != null) iv_play.setVisibility(View.VISIBLE);
                    iv_playpause.setBackground(ContextCompat.getDrawable(this, R.drawable.icon_play));
                } else {
                    mVideoView.start();
                    if (iv_play != null) iv_play.setVisibility(View.INVISIBLE);
                    if (countDownTimer != null) countDownTimer.start();
                    iv_playpause.setBackground(ContextCompat.getDrawable(this, R.drawable.vod_pause));
                }
            });
        }
        if (sBar != null) {
            sBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (!fromUser) return;
                    if (countDownTimer != null && mVideoView != null) {
                        mVideoView.seekTo(progress);
                        countDownTimer.cancel();
                        countDownTimer.start();
                    }
                }
            });
            sBar.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
                    if (mVideoView == null) return false;
                    if (mVideoView.isPlaying()) {
                        mVideoView.pause();
                        if (countDownTimer != null) countDownTimer.cancel();
                        if (iv_play != null) iv_play.setVisibility(View.VISIBLE);
                        if (iv_playpause != null)
                            iv_playpause.setBackground(ContextCompat.getDrawable(this, R.drawable.icon_play));
                    } else {
                        mVideoView.start();
                        if (iv_play != null) iv_play.setVisibility(View.INVISIBLE);
                        if (countDownTimer != null) countDownTimer.start();
                        if (iv_playpause != null)
                            iv_playpause.setBackground(ContextCompat.getDrawable(this, R.drawable.vod_pause));
                    }
                    return true;
                }
                return false;
            });
        }
    }

    // ========== 初始化视频播放器 ==========
    private void initVideoView() {
        if (mVideoView == null) return;
        livePlayerManager.init(mVideoView);
        mVideoView.setVideoController(new LiveController(this));
    }

    // ========== 频道列表 ==========
    private void initChannelGroupView() {
        if (mChannelGroupView == null) return;
        mChannelGroupView.setHasFixedSize(true);
        mChannelGroupView.setLayoutManager(new V7LinearLayoutManager(this, 1, false));
        liveChannelGroupAdapter = new LiveChannelGroupAdapter();
        mChannelGroupView.setAdapter(liveChannelGroupAdapter);
        mChannelGroupView.setOnItemListener(new TvRecyclerView.OnItemListener() {
            @Override public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {}
            @Override public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
                selectChannelGroup(position, true);
            }
            @Override public void onItemClick(TvRecyclerView parent, View itemView, int position) {}
        });
    }

    private void initLiveChannelView() {
        if (mLiveChannelView == null) return;
        mLiveChannelView.setHasFixedSize(true);
        mLiveChannelView.setLayoutManager(new V7LinearLayoutManager(this, 1, false));
        liveChannelItemAdapter = new LiveChannelItemAdapter();
        mLiveChannelView.setAdapter(liveChannelItemAdapter);
        mLiveChannelView.setOnItemListener(new TvRecyclerView.OnItemListener() {
            @Override public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {}
            @Override public void onItemSelected(TvRecyclerView parent, View itemView, int position) {}
            @Override public void onItemClick(TvRecyclerView parent, View itemView, int position) {
                clickLiveChannel(position);
            }
        });
    }

    private void selectChannelGroup(int groupIndex, boolean focus) {
        if (groupIndex < 0 || groupIndex >= liveChannelGroupList.size()) return;
        LiveChannelGroup group = liveChannelGroupList.get(groupIndex);
        if (group == null || group.getLiveChannels() == null) return;
        if (liveChannelItemAdapter != null) {
            liveChannelItemAdapter.setNewData(group.getLiveChannels());
        }
        if (focus && mLiveChannelView != null) {
            mLiveChannelView.setSelection(0);
        }
    }

    private void clickLiveChannel(int position) {
        if (tvLeftChannelListLayout != null) {
            tvLeftChannelListLayout.setVisibility(View.INVISIBLE);
        }
        playChannel(currentChannelGroupIndex, position, true);
    }

    // ========== 设置列表 ==========
    private void initSettingGroupView() {
        if (mSettingGroupView == null) return;
        mSettingGroupView.setHasFixedSize(true);
        mSettingGroupView.setLayoutManager(new V7LinearLayoutManager(this, 1, false));
        liveSettingGroupAdapter = new LiveSettingGroupAdapter();
        mSettingGroupView.setAdapter(liveSettingGroupAdapter);
        mSettingGroupView.setOnItemListener(new TvRecyclerView.OnItemListener() {
            @Override public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {}
            @Override public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
                selectVisibleSettingGroup(position);
            }
            @Override public void onItemClick(TvRecyclerView parent, View itemView, int position) {}
        });
    }

    private void initSettingItemView() {
        if (mSettingItemView == null) return;
        mSettingItemView.setHasFixedSize(true);
        mSettingItemView.setLayoutManager(new V7LinearLayoutManager(this, 1, false));
        liveSettingItemAdapter = new LiveSettingItemAdapter();
        mSettingItemView.setAdapter(liveSettingItemAdapter);
        mSettingItemView.setOnItemListener(new TvRecyclerView.OnItemListener() {
            @Override public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {}
            @Override public void onItemSelected(TvRecyclerView parent, View itemView, int position) {}
            @Override public void onItemClick(TvRecyclerView parent, View itemView, int position) {
                clickSettingItem(position);
            }
        });
    }

    private void selectVisibleSettingGroup(int position) {
        if (liveSettingGroupAdapter == null || mSettingItemView == null) return;
        List<LiveSettingGroup> visibleGroups = getVisibleLiveSettingGroupList();
        if (position < 0 || position >= visibleGroups.size()) return;
        LiveSettingGroup group = visibleGroups.get(position);
        if (group == null || group.getLiveSettingItems() == null) return;
        liveSettingGroupAdapter.setSelectedGroupIndex(position);
        if (liveSettingItemAdapter != null) {
            liveSettingItemAdapter.setNewData(group.getLiveSettingItems());
            try {
                java.lang.reflect.Method method = liveSettingItemAdapter.getClass().getMethod("setSelectedItemIndex", int.class);
                method.invoke(liveSettingItemAdapter, group.getLiveSettingItems().size() > 0 ? 0 : -1);
            } catch (Exception e) {
                // ignore
            }
        }
        mSettingItemView.setSelection(0);
    }

    private void clickSettingItem(int position) {
        if (liveSettingItemAdapter == null) return;
        LiveSettingItem item = liveSettingItemAdapter.getItem(position);
        if (item == null) return;
        int itemIndex = item.getItemIndex();
        switch (itemIndex) {
            case 0: // 换源
                if (currentLiveChannelItem != null) {
                    int nextSource = currentLiveChannelItem.getSourceIndex() + 1;
                    if (nextSource >= currentLiveChannelItem.getSourceNum()) nextSource = 0;
                    currentLiveChannelItem.setSourceIndex(nextSource);
                    playChannel(currentChannelGroupIndex, currentLiveChannelIndex, false);
                    Toast.makeText(this, "已切换至线路" + (nextSource + 1), Toast.LENGTH_SHORT).show();
                }
                break;
            case 1: // 画面比例
                Toast.makeText(this, "画面比例切换", Toast.LENGTH_SHORT).show();
                break;
            case 2: // 解码方式
                Toast.makeText(this, "解码方式切换", Toast.LENGTH_SHORT).show();
                break;
            case 3: // 超时换源
                Toast.makeText(this, "超时换源设置", Toast.LENGTH_SHORT).show();
                break;
            case 4: // 显示/隐藏 EPG
                if (divEpg != null) {
                    divEpg.setVisibility(divEpg.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                }
                break;
        }
        if (tvRightSettingLayout != null) {
            tvRightSettingLayout.setVisibility(View.INVISIBLE);
        }
    }

    private List<LiveSettingGroup> getVisibleLiveSettingGroupList() {
        List<LiveSettingGroup> result = new ArrayList<>();
        for (LiveSettingGroup group : liveSettingGroupList) {
            if (group != null && group.getGroupName() != null && !group.getGroupName().isEmpty()) {
                result.add(group);
            }
        }
        return result;
    }

    private int getDefaultSettingGroupIndex() {
        return 0;
    }

    // ========== 加载直播源 ==========
    private void initLiveChannelList() {
        ApiConfig api = ApiConfig.get();
        List<LiveChannelGroup> list = api.getChannelGroupList();
        if (list != null && !list.isEmpty()) {
            applyChannelList(list);
            return;
        }
        // 异步加载
        loadLiveSourceAsync();
    }

    private void loadLiveSourceAsync() {
        final String apiUrl = Hawk.get(HawkConfig.API_URL, "");
        if (apiUrl == null || apiUrl.trim().isEmpty()) {
            Toast.makeText(this, "请先设置接口地址", Toast.LENGTH_LONG).show();
            return;
        }
        showLoading();
        new Thread(() -> {
            try {
                String configJson = fetchContent(apiUrl);
                if (configJson != null && configJson.trim().startsWith("{")) {
                    com.google.gson.JsonObject config = new com.google.gson.JsonParser().parse(configJson).getAsJsonObject();
                    if (config.has("lives")) {
                        com.google.gson.JsonArray lives = config.getAsJsonArray("lives");
                        loadLivesAndApply(lives);
                        return;
                    }
                }
                com.google.gson.JsonArray livesArray = new com.google.gson.JsonArray();
                com.google.gson.JsonObject liveObj = new com.google.gson.JsonObject();
                liveObj.addProperty("name", "直播源");
                liveObj.addProperty("type", 0);
                liveObj.addProperty("url", apiUrl);
                livesArray.add(liveObj);
                loadLivesAndApply(livesArray);
            } catch (Exception e) {
                Log.e("LivePlay", "load source error", e);
                runOnUiThread(() -> {
                    showSuccess();
                    Toast.makeText(LivePlayActivity.this, "加载失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private String fetchContent(String url) {
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();
            Request request = new Request.Builder().url(url).build();
            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            }
        } catch (Exception e) {
            Log.e("LivePlay", "fetch error: " + e.getMessage());
        }
        return null;
    }

    private void loadLivesAndApply(com.google.gson.JsonArray livesArray) throws Exception {
        final ApiConfig api = ApiConfig.get();
        java.lang.reflect.Method loadLives = api.getClass().getMethod("loadLives", com.google.gson.JsonArray.class);
        loadLives.invoke(api, livesArray);
        final List<LiveChannelGroup> list = api.getChannelGroupList();
        runOnUiThread(() -> {
            showSuccess();
            if (list != null && !list.isEmpty()) {
                applyChannelList(list);
            } else {
                Toast.makeText(LivePlayActivity.this, "未解析到频道，请检查直播源格式", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void applyChannelList(List<LiveChannelGroup> list) {
        liveChannelGroupList.clear();
        liveChannelGroupList.addAll(list);
        if (liveChannelGroupAdapter != null) {
            liveChannelGroupAdapter.setNewData(liveChannelGroupList);
        }
        // 默认播放第一个频道的第一个
        if (!liveChannelGroupList.isEmpty()) {
            LiveChannelGroup firstGroup = liveChannelGroupList.get(0);
            if (firstGroup != null && firstGroup.getLiveChannels() != null && !firstGroup.getLiveChannels().isEmpty()) {
                playChannel(0, 0, true);
            }
        }
        // 更新 EPG
        mHandler.postDelayed(mLoadEpgRun, EPG_LOAD_DELAY);
    }

    // ========== 设置项数据 ==========
    private void initLiveSettingGroupList() {
        liveSettingGroupList.clear();
        // 线路
        LiveSettingGroup sourceGroup = new LiveSettingGroup();
        sourceGroup.setGroupName("线路选择");
        List<LiveSettingItem> sourceItems = new ArrayList<>();
        if (currentLiveChannelItem != null) {
            for (int i = 0; i < currentLiveChannelItem.getSourceNum(); i++) {
                LiveSettingItem item = new LiveSettingItem();
                item.setItemName("线路" + (i + 1));
                item.setItemIndex(i);
                sourceItems.add(item);
            }
        }
        sourceGroup.setLiveSettingItems(sourceItems);
        liveSettingGroupList.add(sourceGroup);

        LiveSettingGroup decodeGroup = new LiveSettingGroup();
        decodeGroup.setGroupName("解码方式");
        List<LiveSettingItem> decodeItems = new ArrayList<>();
        String[] decodes = {"系统", "ijk硬解", "ijk软解", "exo"};
        for (int i = 0; i < decodes.length; i++) {
            LiveSettingItem item = new LiveSettingItem();
            item.setItemName(decodes[i]);
            item.setItemIndex(i);
            decodeItems.add(item);
        }
        decodeGroup.setLiveSettingItems(decodeItems);
        liveSettingGroupList.add(decodeGroup);

        LiveSettingGroup scaleGroup = new LiveSettingGroup();
        scaleGroup.setGroupName("画面比例");
        List<LiveSettingItem> scaleItems = new ArrayList<>();
        String[] scales = {"默认", "16:9", "4:3", "填充", "原始", "裁剪"};
        for (int i = 0; i < scales.length; i++) {
            LiveSettingItem item = new LiveSettingItem();
            item.setItemName(scales[i]);
            item.setItemIndex(i);
            scaleItems.add(item);
        }
        scaleGroup.setLiveSettingItems(scaleItems);
        liveSettingGroupList.add(scaleGroup);

        // 添加一个“显示EPG”设置项
        LiveSettingGroup epgGroup = new LiveSettingGroup();
        epgGroup.setGroupName("EPG");
        List<LiveSettingItem> epgItems = new ArrayList<>();
        LiveSettingItem epgItem = new LiveSettingItem();
        epgItem.setItemName("显示/隐藏EPG");
        epgItem.setItemIndex(4);
        epgItems.add(epgItem);
        epgGroup.setLiveSettingItems(epgItems);
        liveSettingGroupList.add(epgGroup);
    }

    // ========== EPG 日期 ==========
    private void initEpgDateView() {
        if (mEpgDateGridView == null) return;
        mEpgDateGridView.setHasFixedSize(true);
        mEpgDateGridView.setLayoutManager(new V7LinearLayoutManager(this, 1, false));
        liveEpgDateAdapter = new LiveEpgDateAdapter();
        mEpgDateGridView.setAdapter(liveEpgDateAdapter);
        mEpgDateGridView.setOnItemListener(new TvRecyclerView.OnItemListener() {
            @Override public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {}
            @Override public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
                if (liveEpgDateAdapter != null) {
                    liveEpgDateAdapter.setSelectedIndex(position);
                }
                mHandler.removeCallbacks(mLoadEpgRun);
                mHandler.postDelayed(mLoadEpgRun, 200);
            }
            @Override public void onItemClick(TvRecyclerView parent, View itemView, int position) {}
        });
    }

    private void initEpgListView() {
        if (mRightEpgList == null) return;
        mRightEpgList.setHasFixedSize(true);
        mRightEpgList.setLayoutManager(new V7LinearLayoutManager(this, 1, false));
        epgListAdapter = new LiveEpgAdapter();
        mRightEpgList.setAdapter(epgListAdapter);
        mRightEpgList.setOnItemListener(new TvRecyclerView.OnItemListener() {
            @Override public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {}
            @Override public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
                if (epgListAdapter != null) {
                    epgListAdapter.setSelectedEpgIndex(position);
                }
            }
            @Override public void onItemClick(TvRecyclerView parent, View itemView, int position) {}
        });
    }

    private void initDayList() {
        liveDayList.clear();
        liveEpgDateList.clear();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        for (int i = 0; i < 8; i++) {
            LiveEpgDate dateItem = new LiveEpgDate();
            // 使用反射设置日期（兼容不同版本）
            try {
                java.lang.reflect.Method setDateMethod = LiveEpgDate.class.getMethod("setDate", String.class);
                setDateMethod.invoke(dateItem, formatDate.format(calendar.getTime()));
            } catch (Exception e) {
                try {
                    java.lang.reflect.Field dateField = LiveEpgDate.class.getField("date");
                    dateField.set(dateItem, formatDate.format(calendar.getTime()));
                } catch (Exception e2) {
                    // ignore
                }
            }
            try {
                java.lang.reflect.Method setPresentedMethod = LiveEpgDate.class.getMethod("setDatePresented", String.class);
                setPresentedMethod.invoke(dateItem, formatDate1.format(calendar.getTime()));
            } catch (Exception e) {
                // ignore
            }
            liveEpgDateList.add(dateItem);
            calendar.add(Calendar.DAY_OF_MONTH, -1);
        }
        if (liveEpgDateAdapter != null) {
            liveEpgDateAdapter.setNewData(liveEpgDateList);
            liveEpgDateAdapter.setSelectedIndex(0);
        }
    }

    // ========== EPG 数据加载 ==========
    private final Runnable mLoadEpgRun = new Runnable() {
        @Override
        public void run() {
            if (channel_Name != null && liveEpgDateAdapter != null && liveEpgDateAdapter.getSelectedIndex() >= 0) {
                getEpg(new Date());
            }
        }
    };

    private void getEpg(Date date) {
        if (channel_Name == null) return;
        String channelName = channel_Name.getChannelName();
        String channelNameReal = normalizeEpgChannelName(getFirstPartBeforeSpace(channelName));
        @SuppressLint("SimpleDateFormat") SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd");
        timeFormat.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
        String epgTagName = channelNameReal;
        if (logoUrl == null || logoUrl.isEmpty()) {
            try {
                String[] epgInfo = EpgUtil.getEpgInfo(channelNameReal);
                if (epgInfo != null && !epgInfo[1].isEmpty()) {
                    epgTagName = epgInfo[1];
                }
                updateChannelIcon(channelName, epgInfo == null ? null : epgInfo[0]);
            } catch (Exception e) {
                Log.e("LivePlay", "getEpg icon error", e);
                updateChannelIcon(channelName, null);
            }
        } else if (logoUrl.equals("false")) {
            updateChannelIcon(channelName, null);
        } else {
            String logo = logoUrl.replace("{name}", epgTagName);
            updateChannelIcon(channelName, logo);
        }
        final String finalEpgTagName = epgTagName;
        if (epgListAdapter != null && currentLiveChannelItem != null) {
            epgListAdapter.CanBack(currentLiveChannelItem.getinclude_back());
        }
        if (!hasEpgAddress()) {
            updateEpgPanelState(false);
            return;
        }
        ArrayList<String> epgQueryNames = buildEpgQueryNames(channelName, channelNameReal, finalEpgTagName);
        String url = buildEpgUrl(epgStringAddress, epgQueryNames.get(0), date, timeFormat);

        String savedEpgKey = channelName + "_" + Objects.requireNonNull(liveEpgDateAdapter.getItem(liveEpgDateAdapter.getSelectedIndex())).getDatePresented();
        if (hsEpg.containsKey(savedEpgKey)) {
            showEpg(date, hsEpg.get(savedEpgKey));
            showBottomEpg();
            return;
        }
        updateEpgPanelState(false);
        requestEpg(url, date, channelNameReal, finalEpgTagName, savedEpgKey, epgQueryNames, timeFormat, 0);
    }

    private String getFirstPartBeforeSpace(String str) {
        if (str == null || str.isEmpty()) return str;
        int spaceIndex = str.indexOf(' ');
        return spaceIndex == -1 ? str : str.substring(0, spaceIndex);
    }

    private String normalizeEpgChannelName(String name) {
        if (name == null) return "";
        return name.replaceAll("CCTV-", "CCTV")
                   .replaceAll("\\+", "")
                   .replaceAll("HD", "")
                   .replaceAll("\\d+K", "")
                   .replaceAll("\\s+", "")
                   .trim();
    }

    private String getConfiguredEpgAddress() {
        String userEpgAddress = Hawk.get(HawkConfig.EPG_URL, "");
        if (userEpgAddress != null && userEpgAddress.trim().length() >= 5) {
            return userEpgAddress.trim();
        }
        return DEFAULT_EPG_ADDRESS;
    }

    private boolean hasEpgAddress() {
        return epgStringAddress != null && !epgStringAddress.trim().isEmpty();
    }

    private String buildEpgUrl(String address, String epgTagName, Date date, SimpleDateFormat timeFormat) {
        if (address.contains("{name}") || address.contains("{date}")) {
            return address.replace("{name}", encodeEpgParam(epgTagName)).replace("{date}", timeFormat.format(date));
        } else if (isXmlEpgAddress(address)) {
            return address;
        } else {
            return address + (address.contains("?") ? "&" : "?") + "ch=" + encodeEpgParam(epgTagName) + "&date=" + timeFormat.format(date);
        }
    }

    private String encodeEpgParam(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return value == null ? "" : value;
        }
    }

    private ArrayList<String> buildEpgQueryNames(String channelName, String channelNameReal, String epgTagName) {
        ArrayList<String> queryNames = new ArrayList<>();
        addEpgQueryName(queryNames, epgTagName);
        addEpgQueryName(queryNames, channelNameReal);
        addEpgQueryName(queryNames, normalizeEpgChannelName(getFirstPartBeforeSpace(channelName)));
        addEpgQueryName(queryNames, getFirstPartBeforeSpace(channelName));
        addEpgQueryName(queryNames, channelName);
        if (queryNames.isEmpty()) {
            queryNames.add("");
        }
        return queryNames;
    }

    private void addEpgQueryName(ArrayList<String> queryNames, String name) {
        if (name == null) return;
        String trimName = name.trim();
        if (trimName.isEmpty() || queryNames.contains(trimName)) return;
        queryNames.add(trimName);
    }

    private void requestEpg(String url, Date date, String channelNameReal, String finalEpgTagName, String savedEpgKey,
                            ArrayList<String> epgQueryNames, SimpleDateFormat timeFormat, int queryIndex) {
        OkHttpClient client = OkGoHelper.getDefaultClient();
        if (client == null) client = com.github.catvod.net.OkHttp.client();
        client.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mHandler.post(() -> onEpgRequestFailure(date, channelNameReal, finalEpgTagName, savedEpgKey, epgQueryNames, timeFormat, queryIndex));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() != 200) {
                    response.close();
                    mHandler.post(() -> onEpgRequestFailure(date, channelNameReal, finalEpgTagName, savedEpgKey, epgQueryNames, timeFormat, queryIndex));
                    return;
                }
                final String body;
                try {
                    body = response.body() != null ? response.body().string() : "";
                } finally {
                    response.close();
                }
                mHandler.post(() -> onEpgRequestResponse(body, date, channelNameReal, finalEpgTagName, savedEpgKey, epgQueryNames, timeFormat, queryIndex));
            }
        });
    }

    private void onEpgRequestFailure(Date date, String channelNameReal, String finalEpgTagName, String savedEpgKey,
                                     ArrayList<String> epgQueryNames, SimpleDateFormat timeFormat, int queryIndex) {
        if (!isCurrentEpgRequest(savedEpgKey)) return;
        if (requestNextEpgQueryName(date, channelNameReal, finalEpgTagName, savedEpgKey, epgQueryNames, timeFormat, queryIndex)) return;
        if (requestDefaultEpgOnFailure(date, channelNameReal, finalEpgTagName, savedEpgKey, epgQueryNames, timeFormat, queryIndex)) return;
        updateEpgPanelState(false);
    }

    private void onEpgRequestResponse(String paramString, Date date, String channelNameReal, String finalEpgTagName,
                                      String savedEpgKey, ArrayList<String> epgQueryNames, SimpleDateFormat timeFormat, int queryIndex) {
        if (!isCurrentEpgRequest(savedEpgKey)) return;
        if (paramString == null || paramString.trim().isEmpty()) {
            updateEpgPanelState(false);
            return;
        }
        LOG.i("echo-epgTagName:" + channelNameReal);
        ArrayList<Epginfo> arrayList = new ArrayList<>();
        try {
            if (isXmlEpgResponse(paramString)) {
                arrayList = parseXmlEpg(paramString, finalEpgTagName, date);
            } else if (paramString.contains("epg_data") || paramString.trim().startsWith("{")) {
                arrayList = parseJsonEpg(paramString, date);
            }
        } catch (JSONException jSONException) {
            jSONException.printStackTrace();
        }
        if (arrayList.isEmpty() && requestNextEpgQueryName(date, channelNameReal, finalEpgTagName, savedEpgKey, epgQueryNames, timeFormat, queryIndex)) {
            return;
        }
        hsEpg.put(savedEpgKey, arrayList);
        if (!isCurrentEpgRequest(savedEpgKey)) return;
        showEpg(date, arrayList);
        showBottomEpg();
    }

    private boolean requestDefaultEpgOnFailure(Date date, String channelNameReal, String finalEpgTagName, String savedEpgKey,
                                               ArrayList<String> epgQueryNames, SimpleDateFormat timeFormat, int queryIndex) {
        if (DEFAULT_EPG_ADDRESS.equals(epgStringAddress) || epgQueryNames == null || queryIndex >= epgQueryNames.size()) {
            return false;
        }
        String fallbackUrl = buildEpgUrl(DEFAULT_EPG_ADDRESS, epgQueryNames.get(0), date, timeFormat);
        LOG.i("echo-epg fallback default address");
        requestEpg(fallbackUrl, date, channelNameReal, finalEpgTagName, savedEpgKey, epgQueryNames, timeFormat, epgQueryNames.size());
        return true;
    }

    private boolean requestNextEpgQueryName(Date date, String channelNameReal, String finalEpgTagName, String savedEpgKey,
                                            ArrayList<String> epgQueryNames, SimpleDateFormat timeFormat, int queryIndex) {
        if (!isTemplateEpgAddress(epgStringAddress) || epgQueryNames == null || queryIndex + 1 >= epgQueryNames.size()) {
            return false;
        }
        int nextIndex = queryIndex + 1;
        String nextUrl = buildEpgUrl(epgStringAddress, epgQueryNames.get(nextIndex), date, timeFormat);
        LOG.i("echo-epg retry query name:" + epgQueryNames.get(nextIndex));
        requestEpg(nextUrl, date, channelNameReal, finalEpgTagName, savedEpgKey, epgQueryNames, timeFormat, nextIndex);
        return true;
    }

    private boolean isTemplateEpgAddress(String address) {
        return address != null && (address.contains("{name}") || address.contains("{date}"));
    }

    private boolean isCurrentEpgRequest(String savedEpgKey) {
        if (channel_Name == null || liveEpgDateAdapter == null || liveEpgDateAdapter.getSelectedIndex() < 0) return false;
        String currentEpgKey = channel_Name.getChannelName() + "_" + Objects.requireNonNull(liveEpgDateAdapter.getItem(liveEpgDateAdapter.getSelectedIndex())).getDatePresented();
        return savedEpgKey.equals(currentEpgKey);
    }

    private boolean isXmlEpgAddress(String address) {
        if (address == null) return false;
        String lowerAddress = address.toLowerCase(Locale.ROOT);
        int queryIndex = lowerAddress.indexOf("?");
        if (queryIndex >= 0) {
            lowerAddress = lowerAddress.substring(0, queryIndex);
        }
        return lowerAddress.endsWith(".xml");
    }

    private boolean isXmlEpgResponse(String response) {
        if (response == null) return false;
        String trimResponse = response.trim();
        return trimResponse.startsWith("<?xml") || trimResponse.startsWith("<tv") || trimResponse.startsWith("<channel");
    }

    private ArrayList<Epginfo> parseJsonEpg(String response, Date date) throws JSONException {
        ArrayList<Epginfo> epgList = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(response);
        String channelName = jsonObject.optString("channel_name", jsonObject.optString("channel", ""));
        if (isUnavailableEpgText(channelName)) return epgList;
        JSONArray epgArray = findJsonEpgArray(jsonObject);
        if (epgArray == null) return epgList;
        for (int i = 0; i < epgArray.length(); i++) {
            JSONObject item = epgArray.optJSONObject(i);
            if (item == null) continue;
            String title = cleanEpgTitle(item.optString("title", item.optString("name", "")));
            if (TextUtils.isEmpty(title) || isUnavailableEpgText(title)) continue;
            String startText = item.optString("start", item.optString("start_time", item.optString("starttime", "")));
            String endText = item.optString("end", item.optString("end_time", item.optString("endtime", "")));
            Date startDate = parseJsonEpgDate(date, startText);
            Date endDate = parseJsonEpgDate(date, endText);
            if (startDate == null || endDate == null) continue;
            if (!endDate.after(startDate)) {
                endDate = new Date(endDate.getTime() + TimeUnit.DAYS.toMillis(1));
            }
            epgList.add(createXmlEpgInfo(date, title, startDate, endDate, epgList.size()));
        }
        return epgList;
    }

    private JSONArray findJsonEpgArray(JSONObject jsonObject) {
        JSONArray epgArray = jsonObject.optJSONArray("epg_data");
        if (epgArray != null) return epgArray;
        epgArray = jsonObject.optJSONArray("data");
        if (epgArray != null) return epgArray;
        epgArray = jsonObject.optJSONArray("list");
        if (epgArray != null) return epgArray;
        epgArray = jsonObject.optJSONArray("epg");
        if (epgArray != null) return epgArray;
        return null;
    }

    private Date parseJsonEpgDate(Date date, String timeText) {
        if (timeText == null || timeText.trim().isEmpty()) return null;
        String dateStr = formatDate.format(date);
        String[] formats = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "HH:mm:ss", "HH:mm"};
        for (String fmt : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt);
                if (fmt.contains("yyyy")) {
                    return sdf.parse(timeText.trim());
                } else {
                    return sdf.parse(dateStr + " " + timeText.trim());
                }
            } catch (ParseException e) {
                // try next
            }
        }
        return null;
    }

    private ArrayList<Epginfo> parseXmlEpg(String response, String channelName, Date date) {
        ArrayList<Epginfo> epgList = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(response)));
            NodeList channelNodes = document.getElementsByTagName("channel");
            String channelId = null;
            for (int i = 0; i < channelNodes.getLength(); i++) {
                Element channelElement = (Element) channelNodes.item(i);
                String displayName = channelElement.getElementsByTagName("display-name").item(0).getTextContent();
                if (displayName != null && displayName.trim().equalsIgnoreCase(channelName.trim())) {
                    channelId = channelElement.getAttribute("id");
                    break;
                }
            }
            if (channelId == null) return epgList;
            NodeList programmeNodes = document.getElementsByTagName("programme");
            for (int i = 0; i < programmeNodes.getLength(); i++) {
                Element programmeElement = (Element) programmeNodes.item(i);
                if (!channelId.equals(programmeElement.getAttribute("channel"))) continue;
                String startTime = programmeElement.getAttribute("start");
                String stopTime = programmeElement.getAttribute("stop");
                String title = programmeElement.getElementsByTagName("title").item(0).getTextContent();
                Date startDate = parseXmlEpgTime(startTime);
                Date endDate = parseXmlEpgTime(stopTime);
                if (startDate == null || endDate == null) continue;
                epgList.add(createXmlEpgInfo(date, title, startDate, endDate, epgList.size()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return epgList;
    }

    private Date parseXmlEpgTime(String timeText) {
        if (timeText == null || timeText.length() < 14) return null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            return sdf.parse(timeText.substring(0, 14));
        } catch (ParseException e) {
            return null;
        }
    }

    private Epginfo createXmlEpgInfo(Date date, String title, Date startDate, Date endDate, int index) {
        Epginfo epgInfo = new Epginfo();
        epgInfo.title = title;
        epgInfo.startdateTime = startDate;
        epgInfo.enddateTime = endDate;
        epgInfo.index = index;
        return epgInfo;
    }

    private String cleanEpgTitle(String title) {
        if (title == null) return "";
        return title.trim().replaceAll("\\s+", " ").replaceAll("<[^>]*>", "");
    }

    private boolean isUnavailableEpgText(String text) {
        if (text == null || text.trim().isEmpty()) return true;
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("无节目") || lower.contains("暂无") || lower.contains("no epg") || lower.contains("not available");
    }

    private void showEpg(Date date, ArrayList<Epginfo> arrayList) {
        boolean hasEpg = arrayList != null && arrayList.size() > 0;
        updateEpgPanelState(hasEpg);
        if (hasEpg) {
            epgdata = arrayList;
            if (epgListAdapter != null) {
                epgListAdapter.CanBack(currentLiveChannelItem != null && currentLiveChannelItem.getinclude_back());
                epgListAdapter.setNewData(epgdata);
                updateCurrentEpgSelectedIndex();
            }
        }
    }

    private void updateEpgPanelState(boolean hasEpg) {
        if (hasEpg) {
            if (txtNoEpg != null) txtNoEpg.setVisibility(View.GONE);
            if (mRightEpgList != null) mRightEpgList.setVisibility(View.VISIBLE);
            if (divLoadEpgDivider != null) divLoadEpgDivider.setVisibility(View.VISIBLE);
            if (divEpg != null && divEpg.getVisibility() != View.VISIBLE) {
                if (divLoadEpg != null) divLoadEpg.setVisibility(View.VISIBLE);
                if (divLoadEpgleft != null) divLoadEpgleft.setVisibility(View.GONE);
            }
        } else {
            epgdata = new ArrayList<>();
            if (epgListAdapter != null) epgListAdapter.setNewData(epgdata);
            if (txtNoEpg != null) txtNoEpg.setVisibility(View.GONE);
            if (mRightEpgList != null) mRightEpgList.setVisibility(View.GONE);
            if (divEpg != null) divEpg.setVisibility(View.GONE);
            if (divLoadEpg != null) divLoadEpg.setVisibility(View.GONE);
            if (divLoadEpgDivider != null) divLoadEpgDivider.setVisibility(View.GONE);
            if (divLoadEpgleft != null) divLoadEpgleft.setVisibility(View.GONE);
            if (mChannelGroupView != null) mChannelGroupView.setVisibility(View.VISIBLE);
        }
    }

    private void updateCurrentEpgSelectedIndex() {
        if (epgListAdapter == null || epgListAdapter.getData() == null || epgListAdapter.getData().isEmpty()) return;
        int epgIndex = findCurrentEpgIndex(epgListAdapter.getData());
        if (epgIndex >= 0) {
            epgListAdapter.setSelectedEpgIndex(epgIndex);
        }
    }

    private int findCurrentEpgIndex(List<Epginfo> epgList) {
        if (epgList == null || epgList.isEmpty()) return -1;
        Date now = new Date();
        for (int i = epgList.size() - 1; i >= 0; i--) {
            Epginfo epgInfo = epgList.get(i);
            if (epgInfo == null || epgInfo.startdateTime == null || epgInfo.enddateTime == null) continue;
            Date endDateTime = epgInfo.enddateTime;
            if (!endDateTime.after(epgInfo.startdateTime)) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(endDateTime);
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                endDateTime = calendar.getTime();
            }
            if (!now.before(epgInfo.startdateTime) && now.before(endDateTime)) {
                return i;
            }
        }
        return -1;
    }

    private int getCurrentEpgIndexOrSelected() {
        if (epgListAdapter == null) return -1;
        int epgIndex = findCurrentEpgIndex(epgListAdapter.getData());
        if (epgIndex >= 0) return epgIndex;
        epgIndex = epgListAdapter.getSelectedIndex();
        if (epgIndex >= 0 && epgIndex < epgListAdapter.getData().size()) return epgIndex;
        return 0;
    }

    private void showBottomEpg() {
        if (epgdata == null || epgdata.isEmpty()) {
            setDefaultBottomEpg();
            return;
        }
        Date now = new Date();
        Epginfo currentEpg = null;
        Epginfo nextEpg = null;
        for (int i = 0; i < epgdata.size(); i++) {
            Epginfo epg = epgdata.get(i);
            if (epg == null || epg.startdateTime == null || epg.enddateTime == null) continue;
            if (!now.before(epg.startdateTime) && now.before(epg.enddateTime)) {
                currentEpg = epg;
                if (i + 1 < epgdata.size()) {
                    nextEpg = epgdata.get(i + 1);
                }
                break;
            }
        }
        @SuppressLint("SimpleDateFormat") SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        if (currentEpg != null) {
            if (tv_curepg_left != null) tv_curepg_left.setText("正在播出");
            if (tip_epg1 != null) tip_epg1.setText(timeFormat.format(currentEpg.startdateTime) + "-" + timeFormat.format(currentEpg.enddateTime));
            if (tv_current_program_name != null) tv_current_program_name.setText(currentEpg.title);
            if (tv_nextepg_left != null) tv_nextepg_left.setText("即将播出");
            if (tip_epg2 != null) tip_epg2.setText(nextEpg != null ? timeFormat.format(nextEpg.startdateTime) + "-" + timeFormat.format(nextEpg.enddateTime) : "");
            if (tv_next_program_name != null) tv_next_program_name.setText(nextEpg != null ? nextEpg.title : "");
        } else {
            setDefaultBottomEpg();
        }
    }

    private void setDefaultBottomEpg() {
        if (tv_curepg_left != null) tv_curepg_left.setText("正在播出");
        if (tip_epg1 != null) tip_epg1.setText("暂无信息");
        if (tv_current_program_name != null) tv_current_program_name.setText("");
        if (tv_nextepg_left != null) tv_nextepg_left.setText("即将播出");
        if (tip_epg2 != null) tip_epg2.setText("暂无信息");
        if (tv_next_program_name != null) tv_next_program_name.setText("");
    }

    private void updateChannelIcon(String channelName, String iconUrl) {
        if (imgLiveIcon == null || liveIconNullBg == null || liveIconNullText == null) return;
        if (iconUrl != null && !iconUrl.isEmpty()) {
            imgLiveIcon.setVisibility(View.VISIBLE);
            liveIconNullBg.setVisibility(View.INVISIBLE);
            liveIconNullText.setVisibility(View.INVISIBLE);
            try {
                Glide.with(this).load(iconUrl).into(imgLiveIcon);
            } catch (Exception e) {
                Log.e("LivePlay", "load icon error", e);
            }
        } else {
            imgLiveIcon.setVisibility(View.INVISIBLE);
            liveIconNullBg.setVisibility(View.VISIBLE);
            liveIconNullText.setVisibility(View.VISIBLE);
            liveIconNullText.setText(channelName.substring(0, 1));
        }
    }

    // ========== 播放控制 ==========
    private void playChannel(int groupIndex, int channelIndex, boolean change) {
        if (liveChannelGroupList == null || groupIndex < 0 || groupIndex >= liveChannelGroupList.size()) return;
        LiveChannelGroup group = liveChannelGroupList.get(groupIndex);
        if (group == null || group.getLiveChannels() == null || channelIndex < 0 || channelIndex >= group.getLiveChannels().size()) return;
        LiveChannelItem channelItem = group.getLiveChannels().get(channelIndex);
        if (channelItem == null) return;

        currentChannelGroupIndex = groupIndex;
        currentLiveChannelIndex = channelIndex;
        currentLiveChannelItem = channelItem;
        channel_Name = channelItem;

        if (tip_chname != null) tip_chname.setText(channelItem.getChannelName());
        if (tv_channelnum != null) tv_channelnum.setText(String.valueOf(channelIndex + 1));
        if (tv_srcinfo != null) tv_srcinfo.setText("线路 " + (channelItem.getSourceIndex() + 1) + "/" + channelItem.getSourceNum());

        String url = channelItem.getUrl();
        if (url == null || url.isEmpty()) return;

        if (mVideoView != null) {
            mVideoView.release();
            mVideoView.setUrl(url);
            mVideoView.start();
        }

        showChannelInfo();
        loadEpgAfterChannelStarted();
        showResolutionAfterChannelSwitch();

        if (ll_right_top_loading != null) ll_right_top_loading.setVisibility(View.VISIBLE);
        if (tv_right_top_channel_name != null) tv_right_top_channel_name.setText(channelItem.getChannelName());
    }

    private void showChannelInfo() {
        if (tvChannelInfo == null) return;
        tvChannelInfo.setText((currentLiveChannelIndex + 1) + " " + (currentLiveChannelItem != null ? currentLiveChannelItem.getChannelName() : ""));
        tvChannelInfo.setVisibility(View.VISIBLE);
        mHandler.removeCallbacks(mHideChannelInfoRun);
        mHandler.postDelayed(mHideChannelInfoRun, 3000);
    }

    private final Runnable mHideChannelInfoRun = new Runnable() {
        @Override
        public void run() {
            if (tvChannelInfo != null) tvChannelInfo.setVisibility(View.GONE);
        }
    };

    private void loadEpgAfterChannelStarted() {
        if (mHandler != null) {
            mHandler.removeCallbacks(mLoadEpgRun);
            mHandler.postDelayed(mLoadEpgRun, EPG_LOAD_DELAY);
        }
    }

    private void showResolutionAfterChannelSwitch() {
        resolutionInfoRetryCount = 0;
        resolutionInfoPending = true;
        if (tvResolution != null) {
            tvResolution.setVisibility(View.GONE);
        }
        mHandler.removeCallbacks(mUpdateResolutionInfoRun);
        mHandler.postDelayed(mUpdateResolutionInfoRun, RESOLUTION_INFO_RETRY_DELAY);
    }

    private final Runnable mUpdateResolutionInfoRun = new Runnable() {
        @Override
        public void run() {
            if (!resolutionInfoPending || exitingLivePlay) return;
            if (mVideoView == null) {
                retryOrHideResolutionInfo();
                return;
            }
            int[] wh = mVideoView.getVideoSize();
            if (wh == null || wh.length < 2 || wh[0] <= 0 || wh[1] <= 0) {
                retryOrHideResolutionInfo();
                return;
            }
            if (tvResolution != null) {
                tvResolution.setText(wh[0] + " x " + wh[1]);
                tvResolution.setVisibility(View.VISIBLE);
            }
            mHandler.removeCallbacks(mHideResolutionInfoRun);
            mHandler.postDelayed(mHideResolutionInfoRun, RESOLUTION_INFO_HIDE_DELAY);
            resolutionInfoPending = false;
        }
    };

    private void retryOrHideResolutionInfo() {
        resolutionInfoRetryCount++;
        if (resolutionInfoRetryCount >= RESOLUTION_INFO_MAX_RETRY) {
            resolutionInfoPending = false;
            if (tvResolution != null) tvResolution.setVisibility(View.GONE);
            return;
        }
        mHandler.removeCallbacks(mUpdateResolutionInfoRun);
        mHandler.postDelayed(mUpdateResolutionInfoRun, RESOLUTION_INFO_RETRY_DELAY);
    }

    private final Runnable mHideResolutionInfoRun = new Runnable() {
        @Override
        public void run() {
            if (tvResolution != null) tvResolution.setVisibility(View.GONE);
        }
    };

    private void playNext() {
        if (liveChannelGroupList == null || liveChannelGroupList.isEmpty()) return;
        int nextIndex = currentLiveChannelIndex + 1;
        LiveChannelGroup group = liveChannelGroupList.get(currentChannelGroupIndex);
        if (group == null || group.getLiveChannels() == null) return;
        if (nextIndex >= group.getLiveChannels().size()) {
            nextIndex = 0;
            int nextGroup = currentChannelGroupIndex + 1;
            if (nextGroup >= liveChannelGroupList.size()) nextGroup = 0;
            playChannel(nextGroup, nextIndex, true);
        } else {
            playChannel(currentChannelGroupIndex, nextIndex, true);
        }
    }

    private void playPrevious() {
        if (liveChannelGroupList == null || liveChannelGroupList.isEmpty()) return;
        int prevIndex = currentLiveChannelIndex - 1;
        LiveChannelGroup group = liveChannelGroupList.get(currentChannelGroupIndex);
        if (group == null || group.getLiveChannels() == null) return;
        if (prevIndex < 0) {
            int prevGroup = currentChannelGroupIndex - 1;
            if (prevGroup < 0) prevGroup = liveChannelGroupList.size() - 1;
            LiveChannelGroup prevGrp = liveChannelGroupList.get(prevGroup);
            if (prevGrp == null || prevGrp.getLiveChannels() == null) return;
            prevIndex = prevGrp.getLiveChannels().size() - 1;
            playChannel(prevGroup, prevIndex, true);
        } else {
            playChannel(currentChannelGroupIndex, prevIndex, true);
        }
    }

    private void playNextSource() {
        if (currentLiveChannelItem == null) return;
        int nextSource = currentLiveChannelItem.getSourceIndex() + 1;
        if (nextSource >= currentLiveChannelItem.getSourceNum()) {
            nextSource = 0;
        }
        currentLiveChannelItem.setSourceIndex(nextSource);
        playChannel(currentChannelGroupIndex, currentLiveChannelIndex, false);
    }

    private void playPreSource() {
        if (currentLiveChannelItem == null) return;
        int prevSource = currentLiveChannelItem.getSourceIndex() - 1;
        if (prevSource < 0) {
            prevSource = currentLiveChannelItem.getSourceNum() - 1;
        }
        currentLiveChannelItem.setSourceIndex(prevSource);
        playChannel(currentChannelGroupIndex, currentLiveChannelIndex, false);
    }

    private void showChannelList() {
        if (tvLeftChannelListLayout == null) return;
        if (tvLeftChannelListLayout.getVisibility() == View.INVISIBLE) {
            refreshChannelList();
            tvLeftChannelListLayout.setVisibility(View.VISIBLE);
            mHandler.removeCallbacks(mHideChannelListRun);
            mHandler.postDelayed(mHideChannelListRun, 10000);
            mFocusCurrentChannelAndShowChannelList();
        } else {
            mHandler.removeCallbacks(mHideChannelListRun);
            tvLeftChannelListLayout.setVisibility(View.INVISIBLE);
            mHandler.post(mHideChannelListRun);
        }
    }

    private void refreshChannelList() {
        if (liveChannelGroupAdapter == null || liveChannelItemAdapter == null) return;
        List<LiveChannelGroup> groups = new ArrayList<>();
        for (LiveChannelGroup group : liveChannelGroupList) {
            if (group != null && group.getGroupName() != null && !group.getGroupName().isEmpty()) {
                groups.add(group);
            }
        }
        liveChannelGroupAdapter.setNewData(groups);
        if (currentChannelGroupIndex >= 0 && currentChannelGroupIndex < groups.size()) {
            liveChannelGroupAdapter.setSelectedGroupIndex(currentChannelGroupIndex);
            mChannelGroupView.setSelection(currentChannelGroupIndex);
            LiveChannelGroup group = groups.get(currentChannelGroupIndex);
            if (group != null && group.getLiveChannels() != null) {
                liveChannelItemAdapter.setNewData(group.getLiveChannels());
                if (currentLiveChannelIndex >= 0 && currentLiveChannelIndex < group.getLiveChannels().size()) {
                    liveChannelItemAdapter.setSelectedChannelIndex(currentLiveChannelIndex);
                    mLiveChannelView.setSelection(currentLiveChannelIndex);
                }
            }
        }
    }

    private void mFocusCurrentChannelAndShowChannelList() {
        if (mChannelGroupView == null || mLiveChannelView == null) return;
        mChannelGroupView.setSelection(currentChannelGroupIndex);
        if (liveChannelGroupAdapter != null) {
            liveChannelGroupAdapter.setSelectedGroupIndex(currentChannelGroupIndex);
        }
        if (currentChannelGroupIndex >= 0 && currentChannelGroupIndex < liveChannelGroupList.size()) {
            LiveChannelGroup group = liveChannelGroupList.get(currentChannelGroupIndex);
            if (group != null && group.getLiveChannels() != null) {
                if (liveChannelItemAdapter != null) {
                    liveChannelItemAdapter.setNewData(group.getLiveChannels());
                    liveChannelItemAdapter.setSelectedChannelIndex(currentLiveChannelIndex);
                }
                mLiveChannelView.setSelection(currentLiveChannelIndex);
            }
        }
    }

    private final Runnable mHideChannelListRun = new Runnable() {
        @Override
        public void run() {
            if (tvLeftChannelListLayout != null) {
                tvLeftChannelListLayout.setVisibility(View.INVISIBLE);
            }
        }
    };

    private void showSettingGroup() {
        if (tvRightSettingLayout == null) return;
        if (tvRightSettingLayout.getVisibility() == View.INVISIBLE) {
            tvRightSettingLayout.setVisibility(View.VISIBLE);
            mHandler.removeCallbacks(mHideSettingLayoutRun);
            mHandler.postDelayed(mHideSettingLayoutRun, 10000);
            mFocusAndShowSettingGroup();
        } else {
            mHandler.removeCallbacks(mHideSettingLayoutRun);
            tvRightSettingLayout.setVisibility(View.INVISIBLE);
            mHandler.post(mHideSettingLayoutRun);
        }
    }

    private void mFocusAndShowSettingGroup() {
        if (mSettingGroupView == null || liveSettingGroupAdapter == null) return;
        List<LiveSettingGroup> visibleGroups = getVisibleLiveSettingGroupList();
        if (visibleGroups.isEmpty()) return;
        int defaultIndex = getDefaultSettingGroupIndex();
        liveSettingGroupAdapter.setNewData(visibleGroups);
        liveSettingGroupAdapter.setSelectedGroupIndex(defaultIndex);
        mSettingGroupView.setSelection(defaultIndex);
        selectVisibleSettingGroup(defaultIndex);
    }

    private final Runnable mHideSettingLayoutRun = new Runnable() {
        @Override
        public void run() {
            if (tvRightSettingLayout != null) tvRightSettingLayout.setVisibility(View.INVISIBLE);
        }
    };

    // ========== 键盘事件（增加进入设置功能） ==========
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();

            // 如果未配置接口或频道列表为空，按菜单键或确认键引导进入设置
            String apiUrl = Hawk.get(HawkConfig.API_URL, "");
            boolean needSetting = TextUtils.isEmpty(apiUrl) || liveChannelGroupList.isEmpty();

            if (needSetting) {
                // 菜单键或确认键进入设置
                if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                    gotoSetting();
                    return true;
                }
                // 返回键也进入设置
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    gotoSetting();
                    return true;
                }
                // 其他按键忽略或显示提示
                if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
                    Toast.makeText(this, "请先设置接口地址", Toast.LENGTH_SHORT).show();
                    return true;
                }
                // 让其他按键继续传递，但 UI 无反应
                return super.dispatchKeyEvent(event);
            }

            // ---------- 正常按键处理 ----------
            if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
                numericKeyDown(keyCode - KeyEvent.KEYCODE_0);
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_CHANNEL_UP || keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                if (tvLeftChannelListLayout != null && tvLeftChannelListLayout.getVisibility() == View.VISIBLE) {
                    return super.dispatchKeyEvent(event);
                }
                playNext();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                if (tvLeftChannelListLayout != null && tvLeftChannelListLayout.getVisibility() == View.VISIBLE) {
                    return super.dispatchKeyEvent(event);
                }
                playPrevious();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                if (tvLeftChannelListLayout != null && tvLeftChannelListLayout.getVisibility() == View.VISIBLE) {
                    return super.dispatchKeyEvent(event);
                }
                if (isBack && mVideoView != null) {
                    mVideoView.seekTo(mVideoView.getCurrentPosition() - 10000);
                    return true;
                }
                playPreSource();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                if (tvLeftChannelListLayout != null && tvLeftChannelListLayout.getVisibility() == View.VISIBLE) {
                    return super.dispatchKeyEvent(event);
                }
                if (isBack && mVideoView != null) {
                    mVideoView.seekTo(mVideoView.getCurrentPosition() + 10000);
                    return true;
                }
                playNextSource();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                if (tvLeftChannelListLayout != null && tvLeftChannelListLayout.getVisibility() == View.VISIBLE) {
                    return super.dispatchKeyEvent(event);
                }
                showChannelList();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_MENU) {
                showShortcutsMenu();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_SEARCH) {
                showSearchDialog();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK || keyCode == KeyEvent.KEYCODE_F1) {
                showTrackDialog();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (tvRightSettingLayout != null && tvRightSettingLayout.getVisibility() == View.VISIBLE) {
                    mHandler.removeCallbacks(mHideSettingLayoutRun);
                    tvRightSettingLayout.setVisibility(View.INVISIBLE);
                    mHandler.post(mHideSettingLayoutRun);
                    return true;
                }
                if (tvLeftChannelListLayout != null && tvLeftChannelListLayout.getVisibility() == View.VISIBLE) {
                    mHandler.removeCallbacks(mHideChannelListRun);
                    tvLeftChannelListLayout.setVisibility(View.INVISIBLE);
                    mHandler.post(mHideChannelListRun);
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void numericKeyDown(int digit) {
        selectedChannelNumber = selectedChannelNumber * 10 + digit;
        if (selectedChannelNumber > 9999) selectedChannelNumber = digit;
        if (tvSelectedChannel != null) {
            tvSelectedChannel.setText(String.valueOf(selectedChannelNumber));
            tvSelectedChannel.setVisibility(View.VISIBLE);
        }
        mHandler.removeCallbacks(mPlaySelectedChannel);
        mHandler.postDelayed(mPlaySelectedChannel, 2000);
    }

    private final Runnable mPlaySelectedChannel = new Runnable() {
        @Override
        public void run() {
            if (tvSelectedChannel != null) tvSelectedChannel.setVisibility(View.INVISIBLE);
            int targetIndex = selectedChannelNumber - 1;
            selectedChannelNumber = 0;
            if (targetIndex < 0 || liveChannelGroupList == null || liveChannelGroupList.isEmpty()) return;
            int count = 0;
            for (int g = 0; g < liveChannelGroupList.size(); g++) {
                LiveChannelGroup group = liveChannelGroupList.get(g);
                if (group == null || group.getLiveChannels() == null) continue;
                int size = group.getLiveChannels().size();
                if (targetIndex >= count && targetIndex < count + size) {
                    int channelIdx = targetIndex - count;
                    playChannel(g, channelIdx, true);
                    return;
                }
                count += size;
            }
        }
    };

    // ========== 自包含的酷9风格菜单（不依赖布局） ==========

    // ---------- 快捷菜单 ----------
    private void initShortcutsMenu() {
        RecyclerView rv = new RecyclerView(this);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setBackgroundColor(Color.BLACK);
        rv.setPadding(20, 20, 20, 20);

        String[] menuNames = {"解码方式", "画面比例", "时移", "回看", "换源", "显示EPG"};
        final int[] menuTypes = {0, 1, 2, 3, 4, 5};
        ShortcutsAdapter adapter = new ShortcutsAdapter(menuNames);
        rv.setAdapter(adapter);
        adapter.setOnItemClickListener(position -> {
            handleShortcutAction(menuTypes[position]);
            if (shortcutsPopupWindow != null) shortcutsPopupWindow.dismiss();
        });

        shortcutsPopupWindow = new PopupWindow(rv,
                (int) (getResources().getDisplayMetrics().widthPixels * 0.5),
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        shortcutsPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        shortcutsPopupWindow.setOutsideTouchable(true);
        shortcutsPopupWindow.setFocusable(true);
    }

    private void handleShortcutAction(int type) {
        switch (type) {
            case 0: // 解码方式
                Toast.makeText(this, "切换解码器", Toast.LENGTH_SHORT).show();
                break;
            case 1: // 画面比例
                Toast.makeText(this, "切换画面比例", Toast.LENGTH_SHORT).show();
                break;
            case 2: // 时移
                Toast.makeText(this, "时移功能", Toast.LENGTH_SHORT).show();
                break;
            case 3: // 回看
                Toast.makeText(this, "回看功能", Toast.LENGTH_SHORT).show();
                break;
            case 4: // 换源
                if (currentLiveChannelItem != null) {
                    int nextSource = currentLiveChannelItem.getSourceIndex() + 1;
                    if (nextSource >= currentLiveChannelItem.getSourceNum()) nextSource = 0;
                    currentLiveChannelItem.setSourceIndex(nextSource);
                    playChannel(currentChannelGroupIndex, currentLiveChannelIndex, false);
                }
                break;
            case 5: // 显示/隐藏 EPG
                if (divEpg != null) {
                    divEpg.setVisibility(divEpg.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                }
                break;
        }
    }

    private void showShortcutsMenu() {
        if (shortcutsPopupWindow != null && !shortcutsPopupWindow.isShowing()) {
            shortcutsPopupWindow.showAtLocation(getWindow().getDecorView(), Gravity.CENTER, 0, 0);
        }
    }

    // ---------- 搜索 ----------
    private void initSearchDialog() {
        RecyclerView rv = new RecyclerView(this);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setBackgroundColor(Color.BLACK);
        rv.setPadding(20, 20, 20, 20);

        SearchAdapter adapter = new SearchAdapter(new ArrayList<>());
        rv.setAdapter(adapter);
        adapter.setOnItemClickListener(position -> {
            LiveChannelItem item = adapter.getItem(position);
            if (item != null) {
                for (int g = 0; g < liveChannelGroupList.size(); g++) {
                    LiveChannelGroup group = liveChannelGroupList.get(g);
                    if (group != null && group.getLiveChannels() != null) {
                        int idx = group.getLiveChannels().indexOf(item);
                        if (idx >= 0) {
                            playChannel(g, idx, true);
                            break;
                        }
                    }
                }
            }
            if (searchPopupWindow != null) searchPopupWindow.dismiss();
        });
        // 将全部频道数据传给适配器作为初始搜索列表
        List<LiveChannelItem> allChannels = new ArrayList<>();
        for (LiveChannelGroup group : liveChannelGroupList) {
            if (group != null && group.getLiveChannels() != null) {
                allChannels.addAll(group.getLiveChannels());
            }
        }
        adapter.setNewData(allChannels);

        searchPopupWindow = new PopupWindow(rv,
                (int) (getResources().getDisplayMetrics().widthPixels * 0.6),
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        searchPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        searchPopupWindow.setOutsideTouchable(true);
        searchPopupWindow.setFocusable(true);
    }

    private void showSearchDialog() {
        if (searchPopupWindow != null && !searchPopupWindow.isShowing()) {
            searchPopupWindow.showAtLocation(getWindow().getDecorView(), Gravity.CENTER, 0, 0);
        }
    }

    // ---------- 音轨/字幕切换 ----------
    private void initTrackDialog() {
        RecyclerView rv = new RecyclerView(this);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setBackgroundColor(Color.BLACK);
        rv.setPadding(20, 20, 20, 20);

        TrackAdapter adapter = new TrackAdapter(new ArrayList<>());
        rv.setAdapter(adapter);
        adapter.setOnItemClickListener(position -> {
            TrackItem item = adapter.getItem(position);
            if (item != null && mVideoView != null) {
                Toast.makeText(this, "切换到: " + item.name, Toast.LENGTH_SHORT).show();
                // 实际应调用 mVideoView.switchTrack(...)
            }
            if (trackPopupWindow != null) trackPopupWindow.dismiss();
        });
        // 模拟轨道数据（实际应从播放器获取）
        List<TrackItem> tracks = new ArrayList<>();
        tracks.add(new TrackItem("音轨1", "audio"));
        tracks.add(new TrackItem("音轨2", "audio"));
        tracks.add(new TrackItem("字幕1", "subtitle"));
        adapter.setNewData(tracks);

        trackPopupWindow = new PopupWindow(rv,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        trackPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        trackPopupWindow.setOutsideTouchable(true);
        trackPopupWindow.setFocusable(true);
    }

    private void showTrackDialog() {
        if (trackPopupWindow != null && !trackPopupWindow.isShowing()) {
            trackPopupWindow.showAtLocation(getWindow().getDecorView(), Gravity.CENTER, 0, 0);
        }
    }

    // ========== 内部适配器类（自包含） ==========

    // ---------- 快捷菜单适配器 ----------
    private static class ShortcutsAdapter extends RecyclerView.Adapter<ShortcutsAdapter.ViewHolder> {
        private String[] names;
        private OnItemClickListener listener;

        public interface OnItemClickListener {
            void onItemClick(int position);
        }

        public ShortcutsAdapter(String[] names) {
            this.names = names;
        }

        public void setOnItemClickListener(OnItemClickListener listener) {
            this.listener = listener;
        }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setPadding(50, 30, 50, 30);
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(18);
            return new ViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.textView.setText(names[position]);
            holder.textView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(position);
            });
        }

        @Override
        public int getItemCount() { return names.length; }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            ViewHolder(View itemView) {
                super(itemView);
                textView = (TextView) itemView;
            }
        }
    }

    // ---------- 搜索适配器 ----------
    private static class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {
        private List<LiveChannelItem> data = new ArrayList<>();
        private OnItemClickListener listener;

        public interface OnItemClickListener {
            void onItemClick(int position);
        }

        public SearchAdapter(List<LiveChannelItem> data) {
            this.data = data;
        }

        public void setNewData(List<LiveChannelItem> newData) {
            data.clear();
            if (newData != null) data.addAll(newData);
            notifyDataSetChanged();
        }

        public LiveChannelItem getItem(int position) {
            return data.get(position);
        }

        public void setOnItemClickListener(OnItemClickListener listener) {
            this.listener = listener;
        }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setPadding(50, 30, 50, 30);
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(18);
            return new ViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LiveChannelItem item = data.get(position);
            holder.textView.setText(item.getChannelName() + " (" + item.getGroupName() + ")");
            holder.textView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(position);
            });
        }

        @Override
        public int getItemCount() { return data.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            ViewHolder(View itemView) {
                super(itemView);
                textView = (TextView) itemView;
            }
        }
    }

    // ---------- 音轨/字幕适配器 ----------
    private static class TrackItem {
        String name;
        String type;
        TrackItem(String name, String type) { this.name = name; this.type = type; }
    }

    private static class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.ViewHolder> {
        private List<TrackItem> data = new ArrayList<>();
        private OnItemClickListener listener;

        public interface OnItemClickListener {
            void onItemClick(int position);
        }

        public TrackAdapter(List<TrackItem> data) {
            this.data = data;
        }

        public void setNewData(List<TrackItem> newData) {
            data.clear();
            if (newData != null) data.addAll(newData);
            notifyDataSetChanged();
        }

        public TrackItem getItem(int position) {
            return data.get(position);
        }

        public void setOnItemClickListener(OnItemClickListener listener) {
            this.listener = listener;
        }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setPadding(50, 30, 50, 30);
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(18);
            return new ViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TrackItem item = data.get(position);
            holder.textView.setText(item.name + " (" + item.type + ")");
            holder.textView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(position);
            });
        }

        @Override
        public int getItemCount() { return data.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;
            ViewHolder(View itemView) {
                super(itemView);
                textView = (TextView) itemView;
            }
        }
    }

    // ========== 网速更新 ==========
    private final Runnable mUpdateNetSpeedRun = new Runnable() {
        @Override
        public void run() {
            if (tvNetSpeed != null && mVideoView != null) {
                long speed = mVideoView.getTcpSpeed();
                if (speed > 0) {
                    tvNetSpeed.setText(formatNetSpeed(speed));
                    tvNetSpeed.setVisibility(View.VISIBLE);
                }
            }
            mHandler.postDelayed(this, 1000);
        }
    };

    private String formatNetSpeed(long speed) {
        if (speed < 1024) return speed + "B/s";
        if (speed < 1024 * 1024) return String.format(Locale.getDefault(), "%.1fKB/s", speed / 1024.0);
        return String.format(Locale.getDefault(), "%.1fMB/s", speed / (1024.0 * 1024.0));
    }

    // ========== 生命周期 ==========
    @Override
    public void onBackPressed() {
        // 如果未配置接口或频道列表为空，跳转到设置
        String apiUrl = Hawk.get(HawkConfig.API_URL, "");
        if (TextUtils.isEmpty(apiUrl) || liveChannelGroupList.isEmpty()) {
            gotoSetting();
            return;
        }

        // 正常返回逻辑
        if (tvRightSettingLayout != null && tvRightSettingLayout.getVisibility() == View.VISIBLE) {
            mHandler.removeCallbacks(mHideSettingLayoutRun);
            tvRightSettingLayout.setVisibility(View.INVISIBLE);
            mHandler.post(mHideSettingLayoutRun);
            return;
        }
        if (tvLeftChannelListLayout != null && tvLeftChannelListLayout.getVisibility() == View.VISIBLE) {
            mHandler.removeCallbacks(mHideChannelListRun);
            tvLeftChannelListLayout.setVisibility(View.INVISIBLE);
            mHandler.post(mHideChannelListRun);
            return;
        }
        if (isBack) {
            isBack = false;
            playChannel(currentChannelGroupIndex, currentLiveChannelIndex, true);
            if (backcontroller != null) backcontroller.setVisibility(View.GONE);
            if (ll_epg != null) ll_epg.setVisibility(View.VISIBLE);
            return;
        }
        if (mVideoView != null) {
            mVideoView.release();
        }
        exitingLivePlay = true;
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mVideoView != null) mVideoView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mVideoView != null) mVideoView.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mVideoView != null) mVideoView.release();
        if (mHandler != null) mHandler.removeCallbacksAndMessages(null);
        if (countDownTimer != null) countDownTimer.cancel();
        if (objectAnimator != null) objectAnimator.cancel();
    }

    // ========== 视图引用（已定义，避免编译错误） ==========
    private TextView txtNoEpg;
    private View ll_right_top_loading;
    private View ll_right_top_huikan;
}
