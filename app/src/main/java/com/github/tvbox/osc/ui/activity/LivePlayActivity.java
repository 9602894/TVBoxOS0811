package com.github.tvbox.osc.ui.activity;

import static xyz.doikki.videoplayer.util.PlayerUtils.safeTimeMs;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

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
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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

    private List<LiveChannelGroup> liveChannelGroupList = new ArrayList<>();
    private int currentLiveChannelIndex = -1;
    private int currentLiveChangeSourceTimes = 0;
    private LiveChannelItem currentLiveChannelItem = null;
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
    private TextView tv_current_program_name;
    private TextView tv_next_program_name;
    private TextView tv_right_top_channel_name;
    private TextView tv_right_top_epg_name;
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

    public static SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd");
    public static SimpleDateFormat formatDate1 = new SimpleDateFormat("MM-dd");
    public static String day = formatDate.format(new Date());
    public static Date nowday = new Date();

    private boolean isBack = false;
    public static String playUrl;

    private ImageView imgLiveIcon;
    private FrameLayout liveIconNullBg;
    private TextView liveIconNullText;
    private View backcontroller;
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

        tip_chname = findViewById(R.id.tv_channel_bar_name);
        tv_channelnum = findViewById(R.id.tv_channel_bottom_number);
        tip_epg1 = findViewById(R.id.tv_current_program_time);
        tip_epg2 = findViewById(R.id.tv_next_program_time);
        tv_srcinfo = findViewById(R.id.tv_source);
        tv_curepg_left = findViewById(R.id.tv_current_program);
        tv_nextepg_left = findViewById(R.id.tv_next_program);
        tv_current_program_name = findViewById(R.id.tv_current_program_name);
        tv_next_program_name = findViewById(R.id.tv_next_program_name);
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

        objectAnimator = ObjectAnimator.ofFloat(iv_circle_bg, "rotation", 360.0f);
        objectAnimator.setDuration(postTimeout);
        objectAnimator.setRepeatCount(-1);
        objectAnimator.start();

        mEpgDateGridView = findViewById(R.id.mEpgDateGridView);
        Hawk.put(HawkConfig.NOW_DATE, formatDate.format(new Date()));
        day = formatDate.format(new Date());
        nowday = new Date();

        mRightEpgList = findViewById(R.id.lv_epg);
        imgLiveIcon = findViewById(R.id.img_live_icon);
        liveIconNullBg = findViewById(R.id.live_icon_null_bg);
        liveIconNullText = findViewById(R.id.live_icon_null_text);
        if (imgLiveIcon != null) imgLiveIcon.setVisibility(View.INVISIBLE);
        if (liveIconNullText != null) liveIconNullText.setVisibility(View.INVISIBLE);
        if (liveIconNullBg != null) liveIconNullBg.setVisibility(View.INVISIBLE);

        sBar = findViewById(R.id.pb_progressbar);
        tv_currentpos = findViewById(R.id.tv_currentpos);
        backcontroller = findViewById(R.id.backcontroller);
        tv_duration = findViewById(R.id.tv_duration);
        iv_playpause = findViewById(R.id.iv_playpause);
        iv_play = findViewById(R.id.iv_play);
        tvSelectedChannel = findViewById(R.id.tv_selected_channel);

        if (show) {
            if (backcontroller != null) backcontroller.setVisibility(View.VISIBLE);
            if (ll_epg != null) ll_epg.setVisibility(View.GONE);
        } else {
            if (backcontroller != null) backcontroller.setVisibility(View.GONE);
            if (ll_epg != null) ll_epg.setVisibility(View.VISIBLE);
        }

        if (iv_play != null) {
            iv_play.setOnClickListener(arg0 -> {
                if (mVideoView == null) return;
                mVideoView.start();
                iv_play.setVisibility(View.INVISIBLE);
                if (countDownTimer != null) countDownTimer.start();
                if (iv_playpause != null)
                    iv_playpause.setBackground(ContextCompat.getDrawable(LivePlayActivity.context, R.drawable.vod_pause));
            });
        }

        if (iv_playpause != null) {
            iv_playpause.setOnClickListener(arg0 -> {
                if (mVideoView == null) return;
                if (mVideoView.isPlaying()) {
                    mVideoView.pause();
                    if (countDownTimer != null) countDownTimer.cancel();
                    if (iv_play != null) iv_play.setVisibility(View.VISIBLE);
                    iv_playpause.setBackground(ContextCompat.getDrawable(LivePlayActivity.context, R.drawable.icon_play));
                } else {
                    mVideoView.start();
                    if (iv_play != null) iv_play.setVisibility(View.INVISIBLE);
                    if (countDownTimer != null) countDownTimer.start();
                    iv_playpause.setBackground(ContextCompat.getDrawable(LivePlayActivity.context, R.drawable.vod_pause));
                }
            });
        }
        if (sBar != null) {
            sBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onStopTrackingTouch(SeekBar arg0) { }
                @Override public void onStartTrackingTouch(SeekBar arg0) { }
                @Override
                public void onProgressChanged(SeekBar sb, int progress, boolean fromuser) {
                    if (!fromuser) return;
                    if (countDownTimer != null && mVideoView != null) {
                        mVideoView.seekTo(progress);
                        countDownTimer.cancel();
                        countDownTimer.start();
                    }
                }
            });
            sBar.setOnKeyListener((arg0, keycode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keycode == KeyEvent.KEYCODE_DPAD_CENTER || keycode == KeyEvent.KEYCODE_ENTER) {
                        if (mVideoView == null) return false;
                        if (mVideoView.isPlaying()) {
                            mVideoView.pause();
                            if (countDownTimer != null) countDownTimer.cancel();
                            if (iv_play != null) iv_play.setVisibility(View.VISIBLE);
                            if (iv_playpause != null)
                                iv_playpause.setBackground(ContextCompat.getDrawable(LivePlayActivity.context, R.drawable.icon_play));
                        } else {
                            mVideoView.start();
                            if (iv_play != null) iv_play.setVisibility(View.INVISIBLE);
                            if (countDownTimer != null) countDownTimer.start();
                            if (iv_playpause != null)
                                iv_playpause.setBackground(ContextCompat.getDrawable(LivePlayActivity.context, R.drawable.vod_pause));
                        }
                    }
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
