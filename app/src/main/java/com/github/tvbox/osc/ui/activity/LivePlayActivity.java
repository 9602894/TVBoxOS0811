package com.github.tvbox.osc.ui.activity;

import static xyz.doikki.videoplayer.util.PlayerUtils.safeTimeMs;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

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
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import xyz.doikki.videoplayer.player.VideoView;

public class LivePlayActivity extends BaseActivity {
    public static Context context;
    private VideoView mVideoView;
    private View switchChannelSnapshotOverlay;
    private ImageView switchChannelSnapshotImage;
    private TextView tvChannelInfo;
    private TextView tvTime;
    private TextView tvNetSpeed;
    private TextView tvResolution;
    private LinearLayout tvLeftChannelListLayout;
    private TvRecyclerView mChannelGroupView;
    private TvRecyclerView mLiveChannelView;
    private LiveChannelGroupAdapter liveChannelGroupAdapter;
    private LiveChannelItemAdapter liveChannelItemAdapter;

    private LinearLayout tvRightSettingLayout;
    private TvRecyclerView mSettingGroupView;
    private TvRecyclerView mSettingItemView;
    private LiveSettingGroupAdapter liveSettingGroupAdapter;
    private LiveSettingItemAdapter liveSettingItemAdapter;
    private List<LiveSettingGroup> liveSettingGroupList = new ArrayList<>();

    public static int currentChannelGroupIndex = 0;
    private Handler mHandler = new Handler();
    private int resolutionInfoRetryCount = 0;
    private boolean resolutionInfoPending = false;
    private boolean exitingLivePlay = false;
    private static final long EPG_LOAD_DELAY = 1200L;
    private static final int RESOLUTION_INFO_MAX_RETRY = 10;
    private static final long RESOLUTION_INFO_RETRY_DELAY = 300L;
    private static final long RESOLUTION_INFO_HIDE_DELAY = 3000L;
    private static final String DEFAULT_EPG_ADDRESS = "http://epg.51zmt.top:8000/api/diyp/?ch={name}&date={date}";
    private final Runnable mLoadEpgRun = new Runnable() {
        @Override
        public void run() {
            if (channel_Name != null && liveEpgDateAdapter != null && liveEpgDateAdapter.getSelectedIndex() >= 0) {
                getEpg(new Date());
            }
        }
    };

    private List<LiveChannelGroup> liveChannelGroupList = new ArrayList<>();
    private int currentLiveChannelIndex = -1;
    private int currentLiveLookBackIndex = -1;
    private int currentLiveChangeSourceTimes = 0;
    private boolean allowLiveSwitchPlayer = true;
    private LiveChannelItem currentLiveChannelItem = null;
    private LivePlayerManager livePlayerManager = new LivePlayerManager();

    private static LiveChannelItem channel_Name = null;
    private static Hashtable<String, ArrayList<Epginfo>> hsEpg = new Hashtable<>();
    private CountDownTimer countDownTimer;
    private View ll_right_top_loading;
    private View ll_right_top_huikan;
    private View divLoadEpg;
    private View divLoadEpgDivider;
    private View divLoadEpgleft;
    private LinearLayout divEpg;
    RelativeLayout ll_epg;
    TextView tv_channelnum;
    TextView tip_chname;
    TextView tip_epg1;
    TextView tip_epg2;
    TextView tv_srcinfo;
    TextView tv_curepg_left;
    TextView tv_nextepg_left;
    private MyEpgAdapter myAdapter;
    private TextView tv_right_top_channel_name;
    private TextView tv_right_top_epg_name;
    private TextView tv_right_top_type;
    private ImageView iv_circle_bg;
    private TextView tv_shownum;
    private TextView txtNoEpg;
    private ImageView iv_back_bg;

    private ObjectAnimator objectAnimator;
    public String epgStringAddress = "";

    private TvRecyclerView mEpgDateGridView;
    private TvRecyclerView mRightEpgList;
    private LiveEpgDateAdapter liveEpgDateAdapter;
    private LiveEpgAdapter epgListAdapter;

    private List<LiveDayListGroup> liveDayList = new ArrayList<>();
    private List<LiveEpgDate> liveEpgDateList = new ArrayList<>();

    public static SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd");
    public static SimpleDateFormat formatDate1 = new SimpleDateFormat("MM-dd");
    public static String day = formatDate.format(new Date());
    public static Date nowday = new Date();

    private boolean isBack = false;
    private ImageView imgLiveIcon;
    private FrameLayout liveIconNullBg;
    private TextView liveIconNullText;
    private View backcontroller;
    private SeekBar sBar;
    private View iv_playpause;
    private View iv_play;
    private boolean show = false;
    private static final int postTimeout = 6000;

    private int selectedChannelNumber = 0;
    private TextView tvSelectedChannel;
    private String logoUrl = "";

    // 添加缺失的成员变量
    private TextView tv_currentpos;
    private TextView tv_duration;

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_live_play;
    }

    @Override
    protected void init() {
        context = this;
        epgStringAddress = getConfiguredEpgAddress();
        logoUrl = "";

        setLoadSir(findViewById(R.id.live_root));
        try {
            mVideoView = findViewById(R.id.mVideoView);
            switchChannelSnapshotOverlay = findViewById(R.id.switchChannelSnapshotOverlay);
            switchChannelSnapshotImage = findViewById(R.id.switchChannelSnapshotImage);
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
            iv_back_bg = findViewById(R.id.iv_back_bg);
            tv_shownum = findViewById(R.id.tv_shownum);
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

            if (imgLiveIcon != null) imgLiveIcon.setVisibility(View.INVISIBLE);
            if (liveIconNullText != null) liveIconNullText.setVisibility(View.INVISIBLE);
            if (liveIconNullBg != null) liveIconNullBg.setVisibility(View.INVISIBLE);
        } catch (Exception e) {
            Log.e("LivePlayActivity", "init error", e);
        }

        if (iv_circle_bg != null) {
            objectAnimator = ObjectAnimator.ofFloat(iv_circle_bg, "rotation", 360.0f);
            objectAnimator.setDuration(postTimeout);
            objectAnimator.setRepeatCount(-1);
            objectAnimator.start();
        }

        Hawk.put(HawkConfig.NOW_DATE, formatDate.format(new Date()));
        day = formatDate.format(new Date());
        nowday = new Date();

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
    }

    // ========== EPG 相关方法 ==========
    private List<Epginfo> epgdata = new ArrayList<>();

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

    private void updateCurrentEpgSelectedIndex() {
        if (epgListAdapter == null || epgListAdapter.getData() == null || epgListAdapter.getData().isEmpty()) return;
        int epgIndex = findCurrentEpgIndex(epgListAdapter.getData());
        if (epgIndex >= 0) {
            epgListAdapter.setSelectedEpgIndex(epgIndex);
        }
    }

    private void syncCurrentEpgSelection(boolean focus) {
        if (mRightEpgList == null || epgListAdapter == null || epgListAdapter.getData() == null || epgListAdapter.getData().isEmpty()) return;
        int epgIndex = getCurrentEpgIndexOrSelected();
        mRightEpgList.setSelectedPosition(epgIndex);
        mRightEpgList.setSelection(epgIndex);
        epgListAdapter.setSelectedEpgIndex(epgIndex);
        if (focus) {
            epgListAdapter.setFocusedEpgIndex(epgIndex);
            focusEpgPosition(epgIndex);
        } else {
            mRightEpgList.post(() -> mRightEpgList.smoothScrollToPosition(epgIndex));
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

    private String getFirstPartBeforeSpace(String str) {
        if (str == null || str.isEmpty()) return str;
        int spaceIndex = str.indexOf(' ');
        return spaceIndex == -1 ? str : str.substring(0, spaceIndex);
    }

    public void getEpg(Date date) {
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

    private void requestEpg(String url, Date date, String channelNameReal, String finalEpgTagName, String savedEpgKey,
                            ArrayList<String> epgQueryNames, SimpleDateFormat timeFormat, int queryIndex) {
        okhttp3.OkHttpClient client = OkGoHelper.getDefaultClient();
        if (client == null) client = com.github.catvod.net.OkHttp.client();
        client.newCall(new okhttp3.Request.Builder().url(url).build()).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                mHandler.post(() -> onEpgRequestFailure(date, channelNameReal, finalEpgTagName, savedEpgKey, epgQueryNames, timeFormat, queryIndex));
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
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
                // try next format
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

    // ========== 修正后的 normalizeEpgChannelName ==========
    private String normalizeEpgChannelName(String channelName) {
        if (channelName == null) {
            return "";
        }
        String trimName = channelName.trim();
        String compactName = trimName.replace("-", "").replace(" ", "");
        // 酷9风格增强：去除 +、HD、4K/8K 等标识
        compactName = compactName.replaceAll("\\+", "").replaceAll("HD", "").replaceAll("\\d+K", "");
        Matcher cctvMatcher = Pattern.compile("(?i)^(CCTV\\d+(?:\\+|K)?)(?:[\\u4e00-\\u9fa5].*|$)").matcher(compactName);
        if (cctvMatcher.matches()) {
            return cctvMatcher.group(1).toUpperCase(Locale.ROOT);
        }
        if (compactName.toUpperCase(Locale.ROOT).startsWith("CCTV")) {
            return compactName.toUpperCase(Locale.ROOT);
        }
        return trimName;
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
        if (currentEpg != null) {
            @SuppressLint("SimpleDateFormat") SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
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

    private TextView tv_current_program_name;
    private TextView tv_next_program_name;

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

    // ========== UI 交互方法 ==========
    public void divLoadEpgRight(View v) {
        if (divEpg == null) return;
        if (divEpg.getVisibility() == View.VISIBLE) {
            divEpg.setVisibility(View.GONE);
            divLoadEpg.setVisibility(View.VISIBLE);
            divLoadEpgleft.setVisibility(View.GONE);
            if (mChannelGroupView != null) mChannelGroupView.setVisibility(View.VISIBLE);
        } else {
            divEpg.setVisibility(View.VISIBLE);
            divLoadEpg.setVisibility(View.GONE);
            divLoadEpgleft.setVisibility(View.VISIBLE);
            if (mChannelGroupView != null) mChannelGroupView.setVisibility(View.GONE);
            if (liveEpgDateAdapter != null && liveEpgDateAdapter.getSelectedIndex() >= 0) {
                mHandler.postDelayed(mLoadEpgRun, EPG_LOAD_DELAY);
            }
        }
    }

    public void divLoadEpgLeft(View v) {
        if (divEpg == null) return;
        if (divEpg.getVisibility() == View.VISIBLE) {
            divEpg.setVisibility(View.GONE);
            divLoadEpg.setVisibility(View.VISIBLE);
            divLoadEpgleft.setVisibility(View.GONE);
            if (mChannelGroupView != null) mChannelGroupView.setVisibility(View.VISIBLE);
        } else {
            divEpg.setVisibility(View.VISIBLE);
            divLoadEpg.setVisibility(View.GONE);
            divLoadEpgleft.setVisibility(View.VISIBLE);
            if (mChannelGroupView != null) mChannelGroupView.setVisibility(View.GONE);
            if (liveEpgDateAdapter != null && liveEpgDateAdapter.getSelectedIndex() >= 0) {
                mHandler.postDelayed(mLoadEpgRun, EPG_LOAD_DELAY);
            }
        }
    }

    // ========== 生命周期 ==========
    @Override
    public void onBackPressed() {
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

    // ========== 按键事件处理 ==========
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();
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
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                if (tvLeftChannelListLayout != null && tvLeftChannelListLayout.getVisibility() == View.VISIBLE) {
                    return super.dispatchKeyEvent(event);
                }
                showChannelList();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_MENU) {
                showSettingGroup();
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

    private void initVideoView() {
        if (mVideoView == null) return;
        livePlayerManager.init(mVideoView);
        mVideoView.setVideoController(new LiveController(this));
    }

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

    private void clickLiveChannel(int position) {
        if (tvLeftChannelListLayout != null) {
            tvLeftChannelListLayout.setVisibility(View.INVISIBLE);
        }
        playChannel(currentChannelGroupIndex, position, true);
    }

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

    private void clickSettingItem(int position) {
        if (liveSettingItemAdapter == null) return;
        LiveSettingItem item = liveSettingItemAdapter.getItem(position);
        if (item == null) return;
        int itemIndex = item.getItemIndex();
        switch (itemIndex) {
            case 0: // 换源
                break;
            case 1: // 换线路
                break;
            case 2: // 画面比例
                break;
            case 3: // 解码方式
                break;
            case 4: // 超时换源
                break;
            default:
                break;
        }
        if (tvRightSettingLayout != null) {
            tvRightSettingLayout.setVisibility(View.INVISIBLE);
        }
    }

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

    // ========== 修正 initDayList 使用 LiveEpgDate ==========
    private void initDayList() {
        liveDayList.clear();
        liveEpgDateList.clear();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        for (int i = 0; i < 8; i++) {
            LiveEpgDate dateItem = new LiveEpgDate();
            // 使用反射设置 date 字段（兼容不同版本的 LiveEpgDate）
            try {
                java.lang.reflect.Method setDateMethod = LiveEpgDate.class.getMethod("setDate", String.class);
                setDateMethod.invoke(dateItem, formatDate.format(calendar.getTime()));
            } catch (Exception e) {
                // 尝试直接设置字段
                try {
                    java.lang.reflect.Field dateField = LiveEpgDate.class.getField("date");
                    dateField.set(dateItem, formatDate.format(calendar.getTime()));
                } catch (Exception e2) {
                    // 忽略，可能该字段不存在
                    Log.e("LivePlay", "setDate failed", e2);
                }
            }
            // 设置显示日期
            try {
                java.lang.reflect.Method setPresentedMethod = LiveEpgDate.class.getMethod("setDatePresented", String.class);
                setPresentedMethod.invoke(dateItem, formatDate1.format(calendar.getTime()));
            } catch (Exception e) {
                // 忽略
            }
            liveEpgDateList.add(dateItem);
            calendar.add(Calendar.DAY_OF_MONTH, -1);
        }
        if (liveEpgDateAdapter != null) {
            liveEpgDateAdapter.setNewData(liveEpgDateList);
            liveEpgDateAdapter.setSelectedIndex(0);
        }
    }

    private int getDefaultSettingGroupIndex() {
        return 0;
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

    private void initLiveSettingGroupList() {
        liveSettingGroupList.clear();
        // 线路设置
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

        // 解码设置
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

        // 画面比例
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
    }

    // ========== 修正 initLiveChannelList 使用反射 ==========
       private void initLiveChannelList() {
        if (ApiConfig.get().shouldReloadLiveConfig()) {
            loadLiveConfigOnEnter();
            return;
        }
        List<LiveChannelGroup> list = ApiConfig.get().getChannelGroupList();
        if (list.isEmpty()) {
            loadLiveConfigOnEnter();
            return;
        }
        initLiveObj();
        if (list.size() == 1 && list.get(0).getGroupName().startsWith("http://127.0.0.1")) {
            loadProxyLives(list.get(0).getGroupName());
        } else {
            applyLiveChannelGroups(list);
        }
    }

    private boolean loadingLiveConfigOnEnter = false;

    private void loadLiveConfigOnEnter() {
        if (loadingLiveConfigOnEnter) return;
        loadingLiveConfigOnEnter = true;
        showLoading();
        ApiConfig.get().loadLiveConfig(true, new ApiConfig.LoadConfigCallback() {
            @Override
            public void success() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        loadingLiveConfigOnEnter = false;
                        initLiveChannelList();
                        initLiveSettingGroupList();
                    }
                });
            }

            @Override
            public void error(String msg) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        loadingLiveConfigOnEnter = false;
                        setEmptyLiveChannelList();
                    }
                });
            }

            @Override
            public void notice(String msg) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(LivePlayActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    public void loadProxyLives(String url) {
        try {
            Uri parsedUrl = Uri.parse(url);
            url = new String(Base64.decode(parsedUrl.getQueryParameter("ext"), Base64.DEFAULT | Base64.URL_SAFE | Base64.NO_WRAP), "UTF-8");
        } catch (Throwable th) {
            if (!url.startsWith("http://127.0.0.1")) {
                setEmptyLiveChannelList();
                return;
            }
        }
        if (!isValidLiveProxyUrl(url)) {
            setEmptyLiveChannelList();
            return;
        }
        if (!refreshingLiveChannelList) {
            showLoading();
        }

        LOG.i("echo-live-url:"+url);

        if(url.contains(".py") || url.contains(".js")){
            String finalUrl = url;
            Runnable waitResponse = new Runnable() {
                @Override
                public void run() {
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    Future<String> future = executor.submit(new Callable<String>() {
                        @Override
                        public String call() {
                            Spider sp = ApiConfig.get().getLiveCSP(finalUrl);
                            String json=sp.liveContent(finalUrl);
                            return json;
                        }
                    });
                    String sortJson = null;
                    try {
                        sortJson = future.get(ApiConfig.get().getLiveConnectTimeoutSeconds(), TimeUnit.SECONDS);
                    } catch (TimeoutException e) {
                        e.printStackTrace();
                        future.cancel(true);
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    } finally {
                        if (sortJson==null || sortJson.isEmpty()) {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    setEmptyLiveChannelList();
                                }
                            });
                            return;
                        }
                        JsonArray livesArray = TxtSubscribe.parseToJsonArray(sortJson);
                        ApiConfig.get().loadLives(livesArray);
                        List<LiveChannelGroup> list = ApiConfig.get().getChannelGroupList();
                        if (list.isEmpty()) {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    setEmptyLiveChannelList();
                                }
                            });
                            return;
                        }
                        final ArrayList<LiveChannelGroup> loadedGroups = new ArrayList<>(list);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                applyLiveChannelGroups(loadedGroups);
                            }
                        });
                        try {
                            executor.shutdown();
                        } catch (Throwable th) {
                            th.printStackTrace();
                        }
                    }
                }
            };
            Executors.newSingleThreadExecutor().execute(waitResponse);
        } else {
            OkGo.<String>get(url).execute(new AbsCallback<String>() {
                @Override
                public String convertResponse(okhttp3.Response response) throws Throwable {
                    assert response.body() != null;
                    return response.body().string();
                }

                @Override
                public void onSuccess(Response<String> response) {
                    JsonArray livesArray = TxtSubscribe.parseToJsonArray(response.body());
                    ApiConfig.get().loadLives(livesArray);
                    List<LiveChannelGroup> list = ApiConfig.get().getChannelGroupList();
                    if (list.isEmpty()) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                setEmptyLiveChannelList();
                            }
                        });
                        return;
                    }
                    final ArrayList<LiveChannelGroup> loadedGroups = new ArrayList<>(list);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            applyLiveChannelGroups(loadedGroups);
                        }
                    });
                }

                @Override
                public void onError(Response<String> response) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            setEmptyLiveChannelList();
                        }
                    });
                }
            });
        }
    }

    private boolean isValidLiveProxyUrl(String url) {
        if (TextUtils.isEmpty(url)) return false;
        String lowerUrl = url.trim().toLowerCase(Locale.US);
        return lowerUrl.startsWith("http://")
                || lowerUrl.startsWith("https://")
                || lowerUrl.startsWith("rtsp://")
                || lowerUrl.startsWith("rtmp://")
                || lowerUrl.startsWith("rtp://");
    }

    private void applyLiveChannelGroups(List<LiveChannelGroup> groups) {
        liveChannelGroupList.clear();
        liveChannelGroupList.addAll(groups);
        showSuccess();
        initLiveState();
    }

    private void initLiveState() {
        refreshingLiveChannelList = false;
        String lastChannelName = pendingLiveRefreshChannelName == null ? Hawk.get(HawkConfig.LIVE_CHANNEL, "") : pendingLiveRefreshChannelName;
        int sourceIndex = pendingLiveRefreshSourceIndex;
        pendingLiveRefreshChannelName = null;
        pendingLiveRefreshSourceIndex = -1;

        int lastChannelGroupIndex = -1;
        int lastLiveChannelIndex = -1;
        LiveChannelItem lastLiveChannelItem = null;
        for (LiveChannelGroup liveChannelGroup : liveChannelGroupList) {
            ArrayList<LiveChannelItem> groupChannels = liveChannelGroup.getLiveChannels();
            if (groupChannels == null || groupChannels.isEmpty()) {
                continue;
            }
            for (LiveChannelItem liveChannelItem : groupChannels) {
                if (liveChannelItem.getChannelName().equals(lastChannelName)) {
                    lastChannelGroupIndex = liveChannelGroup.getGroupIndex();
                    lastLiveChannelIndex = liveChannelItem.getChannelIndex();
                    lastLiveChannelItem = liveChannelItem;
                    break;
                }
            }
            if (lastChannelGroupIndex != -1) break;
        }
        if (lastChannelGroupIndex == -1) {
            Integer[] cctv1Channel = getFirstChannelByName("CCTV1");
            if (cctv1Channel != null) {
                lastChannelGroupIndex = cctv1Channel[0];
                lastLiveChannelIndex = cctv1Channel[1];
            } else {
                lastChannelGroupIndex = getFirstNoPasswordChannelGroup();
                if (lastChannelGroupIndex == -1)
                    lastChannelGroupIndex = 0;
                lastLiveChannelIndex = 0;
            }
        }
        if (lastLiveChannelItem != null && sourceIndex >= 0 && lastLiveChannelItem.getSourceNum() > 0) {
            lastLiveChannelItem.setSourceIndex(Math.min(sourceIndex, lastLiveChannelItem.getSourceNum() - 1));
        }

        livePlayerManager.init(mVideoView);
        showTime();
        showNetSpeed();
        tvLeftChannelListLayout.setVisibility(View.INVISIBLE);
        tvRightSettingLayout.setVisibility(View.INVISIBLE);

        liveChannelGroupAdapter.clearGroupState();
        liveChannelGroupAdapter.setNewData(new ArrayList<>(liveChannelGroupList));
        currentLiveChannelIndex = -1;
        selectChannelGroup(lastChannelGroupIndex, false, lastLiveChannelIndex);
    }

    private void initLiveObj(){
        catchup = null;
        logoUrl = null;
        int position=ApiConfig.getLiveGroupIndex();
        JsonArray live_groups=Hawk.get(HawkConfig.LIVE_GROUP_LIST,new JsonArray());
        if (live_groups == null || live_groups.size() == 0 || position < 0 || position >= live_groups.size()) {
            return;
        }
        JsonObject livesOBJ = live_groups.get(position).getAsJsonObject();
        String type = livesOBJ.has("type")?livesOBJ.get("type").getAsString():"0";

        if(livesOBJ.has("catchup") && livesOBJ.get("catchup").isJsonObject()){
            catchup = livesOBJ.getAsJsonObject("catchup");
            LOG.i("echo-catchup :"+ catchup.toString());
        }
        if(livesOBJ.has("logo")){
            logoUrl = livesOBJ.get("logo").getAsString();
        }
        if(type.equals("3")){
            String py_jar="";
            if(livesOBJ.has("jar")){
                py_jar=livesOBJ.has("jar")?livesOBJ.get("jar").getAsString():"";
            }else if(livesOBJ.has("api")){
                py_jar=livesOBJ.has("api")?livesOBJ.get("api").getAsString():"";
                String ext="";
                if(livesOBJ.has("ext") && (livesOBJ.get("ext").isJsonObject() || livesOBJ.get("ext").isJsonArray())){
                    ext=livesOBJ.get("ext").toString();
                }else {
                    ext= DefaultConfig.safeJsonString(livesOBJ, "ext", "");
                }
                LOG.i("echo-ext:"+ext);
                if(!ext.isEmpty())py_jar=py_jar+"?extend="+ext;
            }
            ApiConfig.get().setLiveJar(py_jar);
        }
    }

    private Integer[] getFirstChannelByName(String keyword) {
        if (TextUtils.isEmpty(keyword)) return null;
        String upperKeyword = keyword.toUpperCase(Locale.US);
        for (LiveChannelGroup liveChannelGroup : liveChannelGroupList) {
            if (liveChannelGroup == null || isNeedInputPassword(liveChannelGroup.getGroupIndex())) continue;
            ArrayList<LiveChannelItem> groupChannels = liveChannelGroup.getLiveChannels();
            if (groupChannels == null || groupChannels.isEmpty()) continue;
            for (LiveChannelItem item : groupChannels) {
                if (item == null || TextUtils.isEmpty(item.getChannelName())) continue;
                if (item.getChannelName().toUpperCase(Locale.US).contains(upperKeyword)) {
                    return new Integer[]{liveChannelGroup.getGroupIndex(), item.getChannelIndex()};
                }
            }
        }
        return null;
    }

    private int getFirstNoPasswordChannelGroup() {
        for (LiveChannelGroup liveChannelGroup : liveChannelGroupList) {
            if (liveChannelGroup.getGroupPassword().isEmpty())
                return liveChannelGroup.getGroupIndex();
        }
        return -1;
    }

    private boolean isNeedInputPassword(int groupIndex) {
        return !liveChannelGroupList.get(groupIndex).getGroupPassword().isEmpty()
                && !isPasswordConfirmed(groupIndex);
    }

    private boolean isPasswordConfirmed(int groupIndex) {
        for (Integer confirmedNum : channelGroupPasswordConfirmed) {
            if (confirmedNum == groupIndex)
                return true;
        }
        return false;
    }

    private ArrayList<LiveChannelItem> getLiveChannels(int groupIndex) {
        if (!isNeedInputPassword(groupIndex)) {
            return liveChannelGroupList.get(groupIndex).getLiveChannels();
        } else {
            return new ArrayList<>();
        }
    }

    private void showPasswordDialog(int groupIndex, int liveChannelIndex) {
        if (tvLeftChannelListLayout.getVisibility() == View.VISIBLE)
            mHandler.removeCallbacks(mHideChannelListRun);

        LivePasswordDialog dialog = new LivePasswordDialog(this);
        dialog.setOnListener(new LivePasswordDialog.OnListener() {
            @Override
            public void onChange(String password) {
                if (password.equals(liveChannelGroupList.get(groupIndex).getGroupPassword())) {
                    channelGroupPasswordConfirmed.add(groupIndex);
                    loadChannelGroupDataAndPlay(groupIndex, liveChannelIndex);
                } else {
                    Toast.makeText(App.getInstance(), "密码错误", Toast.LENGTH_SHORT).show();
                }

                if (tvLeftChannelListLayout.getVisibility() == View.VISIBLE)
                    mHandler.postDelayed(mHideChannelListRun, postTimeout);
            }

            @Override
            public void onCancel() {
                if (tvLeftChannelListLayout.getVisibility() == View.VISIBLE) {
                    int groupIndex = liveChannelGroupAdapter.getSelectedGroupIndex();
                    liveChannelItemAdapter.setNewData(getLiveChannels(groupIndex));
                }
            }
        });
        dialog.show();
    }

    private void loadChannelGroupDataAndPlay(int groupIndex, int liveChannelIndex) {
        liveChannelGroupAdapter.setSelectedGroupIndex(groupIndex);
        loadChannelGroupData(groupIndex);

        if (liveChannelIndex > -1) {
            clickLiveChannel(liveChannelIndex);
            mChannelGroupView.scrollToPosition(groupIndex);
            mLiveChannelView.scrollToPosition(liveChannelIndex);
        }
    }

    private void loadChannelGroupData(int groupIndex) {
        liveChannelItemAdapter.setNewData(getLiveChannels(groupIndex));
        if (groupIndex == currentChannelGroupIndex) {
            if (currentLiveChannelIndex > -1)
                mLiveChannelView.scrollToPosition(currentLiveChannelIndex);
            liveChannelItemAdapter.setSelectedChannelIndex(currentLiveChannelIndex);
        }
        else {
            mLiveChannelView.scrollToPosition(0);
            liveChannelItemAdapter.setSelectedChannelIndex(-1);
        }
    }

    private void setEmptyLiveChannelList() {
        setEmptyLiveChannelList(true);
    }

    private void setEmptyLiveChannelList(boolean releasePlayer) {
        clearLiveChannelList(releasePlayer);
    }

    private void clearLiveChannelList() {
        clearLiveChannelList(true);
    }

    private void clearLiveChannelList(boolean releasePlayer) {
        refreshingLiveChannelList = false;
        pendingLiveRefreshChannelName = null;
        pendingLiveRefreshSourceIndex = -1;
        currentLiveChannelItem = null;
        currentLiveChannelIndex = -1;
        currentLiveLookBackIndex = -1;
        currentLiveChangeSourceTimes = 0;
        liveChannelGroupList.clear();
        ApiConfig.get().getChannelGroupList().clear();
        mHandler.removeCallbacks(mConnectTimeoutChangeSourceRun);
        mHandler.removeCallbacks(mLoadEpgRun);
        hideSwitchChannelSnapshot();
        if (releasePlayer && mVideoView != null) mVideoView.release();
        showSuccess();
        if (liveChannelGroupAdapter != null) {
            liveChannelGroupAdapter.clearGroupState();
            liveChannelGroupAdapter.setNewData(new ArrayList<>());
        }
        if (liveChannelItemAdapter != null) {
            liveChannelItemAdapter.setFocusedChannelIndex(-1);
            liveChannelItemAdapter.setSelectedChannelIndex(-1);
            liveChannelItemAdapter.setNewData(new ArrayList<>());
        }
        if (tvLeftChannelListLayout != null) tvLeftChannelListLayout.setVisibility(View.INVISIBLE);
        if (tvRightSettingLayout != null) tvRightSettingLayout.setVisibility(View.INVISIBLE);
    }
