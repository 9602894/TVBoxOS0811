package com.github.tvbox.osc.ui.activity;

import static xyz.doikki.videoplayer.util.PlayerUtils.safeTimeMs;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.IntEvaluator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Base64;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.catvod.crawler.Spider;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.App;
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
import com.github.tvbox.osc.ui.dialog.LivePasswordDialog;
import com.github.tvbox.osc.ui.tv.widget.ViewObj;
import com.github.tvbox.osc.util.DefaultConfig;
import com.github.tvbox.osc.util.EpgUtil;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.LOG;
import com.github.tvbox.osc.util.OkGoHelper;
import com.github.tvbox.osc.util.PlayerHelper;
import com.github.tvbox.osc.util.HistoryHelper;
import com.github.tvbox.osc.util.live.TxtSubscribe;
import com.google.gson.JsonArray;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonObject;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;
import com.orhanobut.hawk.Hawk;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import xyz.doikki.videoplayer.exo.ExoMediaSourceHelper;
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
    private static final Pattern CATCHUP_TOKEN_PATTERN = Pattern.compile("(\\$?\\{[^}]*\\})");
    private static final Pattern CATCHUP_TAG_PATTERN = Pattern.compile("\\{([^}]*)\\}");
    private final Runnable mLoadEpgRun = new Runnable() {
        @Override
        public void run() {
            if (channel_Name != null && liveEpgDateAdapter != null && liveEpgDateAdapter.getSelectedIndex() >= 0) {
                getEpg(new Date());
            }
        }
    };
    private boolean firstLiveEpgLoad = true;

    private List<LiveChannelGroup> liveChannelGroupList = new ArrayList<>();
    private int currentLiveChannelIndex = -1;
    private int currentLiveLookBackIndex = -1;
    private int currentLiveChangeSourceTimes = 0;
    private boolean allowLiveSwitchPlayer = true;
    private LiveChannelItem currentLiveChannelItem = null;
    private String pendingLiveRefreshChannelName = null;
    private int pendingLiveRefreshSourceIndex = -1;
    private boolean refreshingLiveChannelList = false;
    private int liveConfigRequestId = 0;
    private LivePlayerManager livePlayerManager = new LivePlayerManager();
    private ArrayList<Integer> channelGroupPasswordConfirmed = new ArrayList<>();

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
    public String epgStringAddress ="";

    private TvRecyclerView mEpgDateGridView;
    private TvRecyclerView mRightEpgList;
    private LiveEpgDateAdapter liveEpgDateAdapter;
    private LiveEpgAdapter epgListAdapter;

    private List<LiveDayListGroup> liveDayList = new ArrayList<>();

    public static SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd");
    public static SimpleDateFormat formatDate1 = new SimpleDateFormat("MM-dd");
    public static String day = formatDate.format(new Date());
    public static Date nowday = new Date();

    private boolean isSHIYI = false;
    private boolean isBack = false;
    private static int shiyi_time_c;
    public static String playUrl;

    private ImageView imgLiveIcon;
    private FrameLayout liveIconNullBg;
    private TextView liveIconNullText;
    SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd");
    private View backcontroller;
    private CountDownTimer countDownTimer3;
    private final int videoWidth = 1920;
    private final int videoHeight = 1080;
    private TextView tv_currentpos;
    private TextView tv_duration;
    private SeekBar sBar;
    private View iv_playpause;
    private View iv_play;
    private boolean show = false;
    private static final int postTimeout = 6000;

    private int selectedChannelNumber = 0;
    private TextView tvSelectedChannel;

    private String logoUrl = "";

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_live_play;
    }

    @Override
    protected void init() {
        context = this;
        epgStringAddress = getConfiguredEpgAddress();
        logoUrl = Hawk.get(HawkConfig.LIVE_LOGO_URL, "");

        setLoadSir(findViewById(R.id.live_root));
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

        tip_chname = (TextView) findViewById(R.id.tv_channel_bar_name);
        tv_channelnum = (TextView) findViewById(R.id.tv_channel_bottom_number);
        tip_epg1 = (TextView) findViewById(R.id.tv_current_program_time);
        tip_epg2 = (TextView) findViewById(R.id.tv_next_program_time);
        tv_srcinfo = (TextView) findViewById(R.id.tv_source);
        tv_curepg_left = (TextView) findViewById(R.id.tv_current_program);
        tv_nextepg_left= (TextView) findViewById(R.id.tv_next_program);
        ll_epg = (RelativeLayout) findViewById(R.id.ll_epg);
        tv_right_top_channel_name = (TextView)findViewById(R.id.tv_right_top_channel_name);
        tv_right_top_epg_name = (TextView)findViewById(R.id.tv_right_top_epg_name);
        iv_circle_bg = (ImageView) findViewById(R.id.iv_circle_bg);
        iv_back_bg = (ImageView) findViewById(R.id.iv_back_bg);
        tv_shownum = (TextView) findViewById(R.id.tv_shownum);
        txtNoEpg = (TextView) findViewById(R.id.txtNoEpg);
        ll_right_top_loading = findViewById(R.id.ll_right_top_loading);
        ll_right_top_huikan = findViewById(R.id.ll_right_top_huikan);
        divLoadEpg = (View) findViewById(R.id.divLoadEpg);
        divLoadEpgDivider = findViewById(R.id.divLoadEpgDivider);
        divLoadEpgleft = (View) findViewById(R.id.divLoadEpgleft);
        divEpg = (LinearLayout) findViewById(R.id.divEPG);

        objectAnimator = ObjectAnimator.ofFloat(iv_circle_bg,"rotation", 360.0f);
        objectAnimator.setDuration(postTimeout);
        objectAnimator.setRepeatCount(-1);
        objectAnimator.start();

        mEpgDateGridView = findViewById(R.id.mEpgDateGridView);
        Hawk.put(HawkConfig.NOW_DATE, formatDate.format(new Date()));
        day=formatDate.format(new Date());
        nowday=new Date();

        mRightEpgList = (TvRecyclerView) findViewById(R.id.lv_epg);
        imgLiveIcon = findViewById(R.id.img_live_icon);
        liveIconNullBg = findViewById(R.id.live_icon_null_bg);
        liveIconNullText = findViewById(R.id.live_icon_null_text);
        if (imgLiveIcon != null) imgLiveIcon.setVisibility(View.INVISIBLE);
        if (liveIconNullText != null) liveIconNullText.setVisibility(View.INVISIBLE);
        if (liveIconNullBg != null) liveIconNullBg.setVisibility(View.INVISIBLE);

        sBar = (SeekBar) findViewById(R.id.pb_progressbar);
        tv_currentpos = (TextView) findViewById(R.id.tv_currentpos);
        backcontroller = (View) findViewById(R.id.backcontroller);
        tv_duration = (TextView) findViewById(R.id.tv_duration);
        iv_playpause = findViewById(R.id.iv_playpause);
        iv_play = findViewById(R.id.iv_play);

        tvSelectedChannel = findViewById(R.id.tv_selected_channel);

        if(show){
            if (backcontroller != null) backcontroller.setVisibility(View.VISIBLE);
            if (ll_epg != null) ll_epg.setVisibility(View.GONE);
        }else{
            if (backcontroller != null) backcontroller.setVisibility(View.GONE);
            if (ll_epg != null) ll_epg.setVisibility(View.VISIBLE);
        }

        if (iv_play != null) {
            iv_play.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    if (mVideoView == null) return;
                    mVideoView.start();
                    iv_play.setVisibility(View.INVISIBLE);
                    if (countDownTimer != null) countDownTimer.start();
                    if (iv_playpause != null) iv_playpause.setBackground(ContextCompat.getDrawable(LivePlayActivity.context, R.drawable.vod_pause));
                }
            });
        }

        if (iv_playpause != null) {
            iv_playpause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    if (mVideoView == null) return;
                    if(mVideoView.isPlaying()){
                        mVideoView.pause();
                        if (countDownTimer != null) countDownTimer.cancel();
                        if (iv_play != null) iv_play.setVisibility(View.VISIBLE);
                        iv_playpause.setBackground(ContextCompat.getDrawable(LivePlayActivity.context, R.drawable.icon_play));
                    }else{
                        mVideoView.start();
                        if (iv_play != null) iv_play.setVisibility(View.INVISIBLE);
                        if (countDownTimer != null) countDownTimer.start();
                        iv_playpause.setBackground(ContextCompat.getDrawable(LivePlayActivity.context, R.drawable.vod_pause));
                    }
                }
            });
        }
        if (sBar != null) {
            sBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onStopTrackingTouch(SeekBar arg0) { }
                @Override
                public void onStartTrackingTouch(SeekBar arg0) { }
                @Override
                public void onProgressChanged(SeekBar sb, int progress, boolean fromuser) {
                    if (!fromuser) return;
                    if(countDownTimer!=null && mVideoView != null){
                        mVideoView.seekTo(progress);
                        countDownTimer.cancel();
                        countDownTimer.start();
                    }
                }
            });
            sBar.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View arg0, int keycode, KeyEvent event) {
                    if(event.getAction()==KeyEvent.ACTION_DOWN){
                        if(keycode==KeyEvent.KEYCODE_DPAD_CENTER||keycode==KeyEvent.KEYCODE_ENTER){
                            if (mVideoView == null) return false;
                            if(mVideoView.isPlaying()){
                                mVideoView.pause();
                                if (countDownTimer != null) countDownTimer.cancel();
                                if (iv_play != null) iv_play.setVisibility(View.VISIBLE);
                                if (iv_playpause != null) iv_playpause.setBackground(ContextCompat.getDrawable(LivePlayActivity.context, R.drawable.icon_play));
                            }else{
                                mVideoView.start();
                                if (iv_play != null) iv_play.setVisibility(View.INVISIBLE);
                                if (countDownTimer != null) countDownTimer.start();
                                if (iv_playpause != null) iv_playpause.setBackground(ContextCompat.getDrawable(LivePlayActivity.context, R.drawable.vod_pause));
                            }
                        }
                    }
                    return false;
                }
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
        Hawk.put(HawkConfig.PLAYER_IS_LIVE,true);
    }

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
            if (epgInfo == null || epgInfo.startdateTime == null || epgInfo.enddateTime == null) {
                continue;
            }
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
            mRightEpgList.post(new Runnable() {
                @Override
                public void run() {
                    mRightEpgList.smoothScrollToPosition(epgIndex);
                }
            });
        }
    }

    private void updateEpgPanelState(boolean hasEpg) {
        if (hasEpg) {
            if (txtNoEpg != null) txtNoEpg.setVisibility(View.GONE);
            if (mRightEpgList != null) mRightEpgList.setVisibility(View.VISIBLE);
            if (divLoadEpgDivider != null) divLoadEpgDivider.setVisibility(View.VISIBLE);
            if (divEpg.getVisibility() != View.VISIBLE) {
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
        if (str == null || str.isEmpty()) {
            return str;
        }
        int spaceIndex = str.indexOf(' ');
        if (spaceIndex == -1) {
            return str;
        } else {
            return str.substring(0, spaceIndex);
        }
    }

    public void getEpg(Date date) {
        if (channel_Name == null) return;
        String channelName = channel_Name.getChannelName();
        String channelNameReal = normalizeEpgChannelName(getFirstPartBeforeSpace(channelName));
        @SuppressLint("SimpleDateFormat") SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd");
        timeFormat.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
        String epgTagName = channelNameReal;
        if (logoUrl==null || logoUrl.isEmpty()){
            String[] epgInfo = EpgUtil.getEpgInfo(channelNameReal);
            if (epgInfo != null && !epgInfo[1].isEmpty()) {
                epgTagName = epgInfo[1];
            }
            updateChannelIcon(channelName, epgInfo == null ? null : epgInfo[0]);
        }else if(logoUrl.equals("false")){
            updateChannelIcon(channelName, null);
        }else {
            String logo= logoUrl.replace("{name}",epgTagName);
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
        if (hsEpg.containsKey(savedEpgKey)){
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
        } else if(isXmlEpgAddress(address)){
            return address;
        }else {
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
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onEpgRequestFailure(date, channelNameReal, finalEpgTagName, savedEpgKey, epgQueryNames, timeFormat, queryIndex);
                    }
                });
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                if (response.code() != 200) {
                    response.close();
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            onEpgRequestFailure(date, channelNameReal, finalEpgTagName, savedEpgKey, epgQueryNames, timeFormat, queryIndex);
                        }
                    });
                    return;
                }
                final String body;
                try {
                    body = response.body() != null ? response.body().string() : "";
                } finally {
                    response.close();
                }
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onEpgRequestResponse(body, date, channelNameReal, finalEpgTagName, savedEpgKey, epgQueryNames, timeFormat, queryIndex);
                    }
                });
            }
        });
    }

    private void onEpgRequestFailure(Date date, String channelNameReal, String finalEpgTagName, String savedEpgKey,
                                     ArrayList<String> epgQueryNames, SimpleDateFormat timeFormat, int queryIndex) {
        if (!isCurrentEpgRequest(savedEpgKey)) return;
        if (requestNextEpgQueryName(date, channelNameReal, finalEpgTagName, savedEpgKey, epgQueryNames, timeFormat, queryIndex)) {
            return;
        }
        if (requestDefaultEpgOnFailure(date, channelNameReal, finalEpgTagName, savedEpgKey, epgQueryNames, timeFormat, queryIndex)) {
            return;
        }
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
        if (address == null) {
            return false;
        }
        String lowerAddress = address.toLowerCase(Locale.ROOT);
        int queryIndex = lowerAddress.indexOf("?");
        if (queryIndex >= 0) {
            lowerAddress = lowerAddress.substring(0, queryIndex);
        }
        return lowerAddress.endsWith(".xml");
    }

    private boolean isXmlEpgResponse(String response) {
        if (response == null) {
            return false;
        }
        String trimResponse = response.trim();
        return trimResponse.startsWith("<?xml") || trimResponse.startsWith("<tv") || trimResponse.startsWith("<channel");
    }

    private ArrayList<Epginfo> parseJsonEpg(String response, Date date) throws JSONException {
        ArrayList<Epginfo> epgList = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(response);
        String channelName = jsonObject.optString("channel_name", jsonObject.optString("channel", ""));
        if (isUnavailableEpgText(channelName)) {
            return epgList;
        }
        JSONArray epgArray = findJsonEpgArray(jsonObject);
        if (epgArray == null) {
            return epgList;
        }
        for (int i = 0; i < epgArray.length(); i++) {
            JSONObject item = epgArray.optJSONObject(i);
            if (item == null) {
                continue;
            }
            String title = cleanEpgTitle(item.optString("title", item.optString("name", "")));
            if (TextUtils.isEmpty(title) || isUnavailableEpgText(title)) {
                continue;
            }
            String startText = item.optString("start", item.optString("start_time", item.optString("starttime", "")));
            String endText = item.optString("end", item.optString("end_time", item.optString("endtime", "")));
            Date startDate = parseJsonEpgDate(date, startText);
            Date endDate = parseJsonEpgDate(date, endText);
            if (startDate == null || endDate == null) {
                continue;
            }
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

    private String normalizeEpgChannelName(String name) {
        if (name == null) return "";
        return name.replaceAll("CCTV-", "CCTV")
                   .replaceAll("\+", "")
                   .replaceAll("HD", "")
                   .replaceAll("\d+K", "")
                   .replaceAll("\s+", "")
                   .trim();
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
            com.github.tvbox.osc.util.FastImgUtil.loadImage(this, iconUrl, imgLiveIcon);
        } else {
            imgLiveIcon.setVisibility(View.INVISIBLE);
            liveIconNullBg.setVisibility(View.VISIBLE);
            liveIconNullText.setVisibility(View.VISIBLE);
            liveIconNullText.setText(channelName.substring(0, 1));
        }
    }

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
        if (mVideoView != null) {
            mVideoView.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mVideoView != null) {
            mVideoView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mVideoView != null) {
            mVideoView.release();
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (objectAnimator != null) {
            objectAnimator.cancel();
        }
    }

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
            @Override
            public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {
            }
            @Override
            public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
                selectChannelGroup(position, true);
            }
            @Override
            public void onItemClick(TvRecyclerView parent, View itemView, int position) {
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

    private void initLiveChannelView() {
        if (mLiveChannelView == null) return;
        mLiveChannelView.setHasFixedSize(true);
        mLiveChannelView.setLayoutManager(new V7LinearLayoutManager(this, 1, false));
        liveChannelItemAdapter = new LiveChannelItemAdapter();
        mLiveChannelView.setAdapter(liveChannelItemAdapter);
        mLiveChannelView.setOnItemListener(new TvRecyclerView.OnItemListener() {
            @Override
            public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {
            }
            @Override
            public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
            }
            @Override
            public void onItemClick(TvRecyclerView parent, View itemView, int position) {
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
            @Override
            public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {
            }
            @Override
            public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
                selectVisibleSettingGroup(position);
            }
            @Override
            public void onItemClick(TvRecyclerView parent, View itemView, int position) {
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
            liveSettingItemAdapter.setSelectedItemIndex(group.getLiveSettingItems().size() > 0 ? 0 : -1);
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
            @Override
            public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {
            }
            @Override
            public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
            }
            @Override
            public void onItemClick(TvRecyclerView parent, View itemView, int position) {
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
            case 0:
                // 换源
                break;
            case 1:
                // 换线路
                break;
            case 2:
                // 画面比例
                break;
            case 3:
                // 解码方式
                break;
            case 4:
                // 超时换源
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
            @Override
            public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {
            }
            @Override
            public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
                if (liveEpgDateAdapter != null) {
                    liveEpgDateAdapter.setSelectedIndex(position);
                }
                mHandler.removeCallbacks(mLoadEpgRun);
                mHandler.postDelayed(mLoadEpgRun, 200);
            }
            @Override
            public void onItemClick(TvRecyclerView parent, View itemView, int position) {
            }
        });
    }

    private void initEpgListView() {
        if (mRightEpgList == null) return;
        mRightEpgList.setHasFixedSize(true);
        mRightEpgList.setLayoutManager(new V7LinearLayoutManager(this, 1, false));
        epgListAdapter = new LiveEpgAdapter();
        mRightEpgList.setAdapter(epgListAdapter);
        mRightEpgList.setOnItemListener(new TvRecyclerView.OnItemListener() {
            @Override
            public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {
            }
            @Override
            public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
                if (epgListAdapter != null) {
                    epgListAdapter.setSelectedEpgIndex(position);
                }
            }
            @Override
            public void onItemClick(TvRecyclerView parent, View itemView, int position) {
            }
        });
    }

    private void initDayList() {
        liveDayList.clear();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        for (int i = 0; i < 8; i++) {
            LiveDayListGroup dayGroup = new LiveDayListGroup();
            dayGroup.setDatePresented(formatDate1.format(calendar.getTime()));
            dayGroup.setDate(formatDate.format(calendar.getTime()));
            liveDayList.add(dayGroup);
            calendar.add(Calendar.DAY_OF_MONTH, -1);
        }
        if (liveEpgDateAdapter != null) {
            liveEpgDateAdapter.setNewData(liveDayList);
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

    private void initLiveChannelList() {
        liveChannelGroupList = ApiConfig.get().getLiveChannelGroupList();
        if (liveChannelGroupList == null || liveChannelGroupList.isEmpty()) {
            Toast.makeText(this, "暂无直播频道", Toast.LENGTH_SHORT).show();
            return;
        }
        int lastGroup = Hawk.get(HawkConfig.LIVE_GROUP_INDEX, 0);
        int lastChannel = Hawk.get(HawkConfig.LIVE_CHANNEL_INDEX, 0);
        if (lastGroup >= liveChannelGroupList.size()) lastGroup = 0;
        LiveChannelGroup group = liveChannelGroupList.get(lastGroup);
        if (group == null || group.getLiveChannels() == null || lastChannel >= group.getLiveChannels().size()) {
            lastChannel = 0;
        }
        playChannel(lastGroup, lastChannel, true);
    }

    private final Runnable mUpdateTimeRun = new Runnable() {
        @Override
        public void run() {
            if (tvTime != null) {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                tvTime.setText(sdf.format(new Date()));
                tvTime.setVisibility(View.VISIBLE);
            }
            mHandler.postDelayed(this, 1000);
        }
    };

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

    private void focusEpgPosition(int position) {
        if (mRightEpgList == null) return;
        mRightEpgList.setSelection(position);
    }
}
