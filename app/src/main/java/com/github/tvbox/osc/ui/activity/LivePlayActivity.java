package com.github.tvbox.osc.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.Epginfo;
import com.github.tvbox.osc.bean.LiveChannelGroup;
import com.github.tvbox.osc.bean.LiveChannelItem;
import com.github.tvbox.osc.bean.LiveSettingGroup;
import com.github.tvbox.osc.bean.LiveSettingItem;
import com.github.tvbox.osc.bean.ShortcutsBean;
import com.github.tvbox.osc.player.controller.LiveController;
import com.github.tvbox.osc.ui.adapter.LiveChannelGroupAdapter;
import com.github.tvbox.osc.ui.adapter.LiveChannelItemAdapter;
import com.github.tvbox.osc.ui.adapter.LiveEpgAdapter;
import com.github.tvbox.osc.ui.adapter.LiveSettingGroupAdapter;
import com.github.tvbox.osc.ui.adapter.LiveSettingItemAdapter;
import com.github.tvbox.osc.ui.adapter.SearchChannelAdapter;
import com.github.tvbox.osc.ui.adapter.SearchKeyboardAdapter;
import com.github.tvbox.osc.ui.adapter.ShortcutsMenuAdapter;
import com.github.tvbox.osc.ui.adapter.TrackListAdapter;
import com.github.tvbox.osc.ui.tv.CustomView.CustomRecyclerView;
import com.github.tvbox.osc.ui.tv.CustomView.MarqueeTextView;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.live.TxtSubscribe;
import com.github.tvbox.osc.util.live.M3U8Subscribe;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;
import com.orhanobut.hawk.Hawk;

import java.io.File;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import xyz.doikki.videoplayer.player.VideoView;

/**
 * TVBoxOS + Ku9 Complete Integration - LivePlayActivity
 */
public class LivePlayActivity extends BaseActivity {

    private VideoView mVideoView;
    private LiveController mController;
    private ProgressBar loadingProgressBar;
    private FrameLayout switchChannelSnapshotOverlay;
    private ImageView switchChannelSnapshotImage;

    private TextView tvSelectedChannel, tvTime, tvNetSpeed, tvResolution;
    private LinearLayout tvTimeGroup;
    private LinearLayout tvBottomLayout;
    private ImageView tvLogo;
    private MarqueeTextView tvChannelName;
    private TextView tvChannelCollect, tvCurrentProgramName, tvCurrentProgramTime, tvNextProgramName, tvNextProgramTime;

    private LinearLayout mPoPuLayout, mShortcutsMenuLayout, mLargeChannelGroupLeftLayout;
    private CustomRecyclerView mShortcutsMenuGridView, mFirstGroupGridView, mGroupGridView, mChannelGridView, mEpgGridView;
    private LinearLayout mChannelGroupLeftLayout, mDivLeft, mChannelLeftLayout, mEpgLeftLayout;

    private LinearLayout mSearchLayout;
    private EditText etSearch;
    private RecyclerView rvSearchResult, rvKeyboard;

    private LinearLayout llSeekBar;
    private SeekBar sbProgress;
    private TextView tvSeekTimeStart, tvSeekTimeEnd, btnSeekBack10, btnSeekForward10;

    private LinearLayout mSettingLayout;
    private CustomRecyclerView mSettingGroupView, mSettingItemView;

    private LinearLayout mPlayMessageLayout;
    private TextView tvVideoUrl, tvVideoFormat, tvVideoCodec, tvVideoResolution, tvAudioCodec, tvPlayerConfig;

    private LinearLayout mTrackLayout;
    private RecyclerView rvTrackList;
    private LinearLayout mThemeLayout;
    private RecyclerView rvThemeColors;

    private LiveChannelGroupAdapter firstGroupAdapter, groupAdapter;
    private LiveChannelItemAdapter channelAdapter;
    private LiveEpgAdapter epgAdapter;
    private LiveSettingGroupAdapter settingGroupAdapter;
    private LiveSettingItemAdapter settingItemAdapter;
    private ShortcutsMenuAdapter shortcutsMenuAdapter;
    private SearchChannelAdapter searchChannelAdapter;
    private SearchKeyboardAdapter searchKeyboardAdapter;
    private TrackListAdapter trackListAdapter;

    private List<LiveChannelGroup> liveChannelGroupList = new ArrayList<>();
    private List<LiveChannelItem> currentChannelList = new ArrayList<>();
    private List<Epginfo> currentEpgList = new ArrayList<>();
    private List<LiveSettingGroup> settingGroupList = new ArrayList<>();
    private List<ShortcutsBean> shortcutsList = new ArrayList<>();
    private List<LiveChannelItem> searchResultList = new ArrayList<>();
    private List<LiveChannelGroup> firstGroupList = new ArrayList<>();

    private int currentGroupIndex = 0, currentChannelIndex = 0, currentLineIndex = 0;
    private int currentFirstGroupIndex = 0, currentSettingGroupIndex = 0, currentSettingItemIndex = 0;
    private int selectedChannelNum = 0;

    private boolean isBackLook = false, isTimeShift = false, channelListIsShow = false, bottomInfoIsShow = false;
    private boolean isSearchShow = false, isSettingShow = false, isPlayMessageShow = false;
    private boolean isTrackShow = false, isThemeShow = false, isEpgShow = false, isPlaying = false;
    private boolean isAutoChangeLine = true, isAutoChangeSource = true;

    private long backLookStartTime = 0, backLookEndTime = 0, currentProgress = 0, totalDuration = 0;
    private String epgUrl = "";

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Timer netSpeedTimer;
    private Runnable mHideChannelListRun, mHideBottomInfoRun, mHideSelectedChannelRun, timeUpdateRun;

    private List<String> collectList = new ArrayList<>();
    private static final String COLLECT_KEY = "live_collect_list";
    private List<LiveChannelItem> localVideoList = new ArrayList<>();
    private static final String LOCAL_VIDEO_DIR = "videoFile";

    @Override protected int getLayoutResID() { return R.layout.activity_live_play; }

    @Override protected void init() {
        initView(); initPlayer(); initAdapters(); initData(); initListener();
        initShortcuts(); initSettingGroups(); startTimeUpdate(); startNetSpeedTimer(); loadCollect();
    }

    private void initView() {
        mVideoView = findViewById(R.id.mVideoView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        switchChannelSnapshotOverlay = findViewById(R.id.switchChannelSnapshotOverlay);
        switchChannelSnapshotImage = findViewById(R.id.switchChannelSnapshotImage);
        tvSelectedChannel = findViewById(R.id.tv_selected_channel);
        tvTime = findViewById(R.id.tvTime);
        tvNetSpeed = findViewById(R.id.tvNetSpeed);
        tvResolution = findViewById(R.id.tvResolution);
        tvTimeGroup = findViewById(R.id.tvTimeGroup);

        tvBottomLayout = findViewById(R.id.tvBottomLayout);
        tvLogo = findViewById(R.id.tv_logo);
        tvChannelName = findViewById(R.id.tv_channel_name);
        tvChannelCollect = findViewById(R.id.tv_channel_collect);
        tvCurrentProgramName = findViewById(R.id.tv_current_program_name);
        tvCurrentProgramTime = findViewById(R.id.tv_current_program_time);
        tvNextProgramName = findViewById(R.id.tv_next_program_name);
        tvNextProgramTime = findViewById(R.id.tv_next_program_time);

        mPoPuLayout = findViewById(R.id.mPoPuLayout);
        mShortcutsMenuLayout = findViewById(R.id.mShortcutsMenuLayout);
        mShortcutsMenuGridView = findViewById(R.id.mShortcutsMenuGridView);
        mLargeChannelGroupLeftLayout = findViewById(R.id.mLargeChannelGroupLeftLayout);
        mFirstGroupGridView = findViewById(R.id.mFirstGroupGridView);
        mChannelGroupLeftLayout = findViewById(R.id.mChannelGroupLeftLayout);
        mGroupGridView = findViewById(R.id.mGroupGridView);
        mDivLeft = findViewById(R.id.mDivLeft);
        mChannelLeftLayout = findViewById(R.id.mChannelLeftLayout);
        mChannelGridView = findViewById(R.id.mChannelGridView);
        mEpgLeftLayout = findViewById(R.id.mEpgLeftLayout);
        mEpgGridView = findViewById(R.id.mEpgGridView);

        mSearchLayout = findViewById(R.id.mSearchLayout);
        etSearch = findViewById(R.id.etSearch);
        rvSearchResult = findViewById(R.id.rvSearchResult);
        rvKeyboard = findViewById(R.id.rvKeyboard);

        llSeekBar = findViewById(R.id.llSeekBar);
        sbProgress = findViewById(R.id.sbProgress);
        tvSeekTimeStart = findViewById(R.id.tvSeekTimeStart);
        tvSeekTimeEnd = findViewById(R.id.tvSeekTimeEnd);
        btnSeekBack10 = findViewById(R.id.btnSeekBack10);
        btnSeekForward10 = findViewById(R.id.btnSeekForward10);

        mSettingLayout = findViewById(R.id.mSettingLayout);
        mSettingGroupView = findViewById(R.id.mSettingGroupView);
        mSettingItemView = findViewById(R.id.mSettingItemView);

        mPlayMessageLayout = findViewById(R.id.mPlayMessageLayout);
        tvVideoUrl = findViewById(R.id.tvVideoUrl);
        tvVideoFormat = findViewById(R.id.tvVideoFormat);
        tvVideoCodec = findViewById(R.id.tvVideoCodec);
        tvVideoResolution = findViewById(R.id.tvVideoResolution);
        tvAudioCodec = findViewById(R.id.tvAudioCodec);
        tvPlayerConfig = findViewById(R.id.tvPlayerConfig);

        mTrackLayout = findViewById(R.id.mTrackLayout);
        rvTrackList = findViewById(R.id.rvTrackList);
        mThemeLayout = findViewById(R.id.mThemeLayout);
        rvThemeColors = findViewById(R.id.rvThemeColors);
    }

    private void initPlayer() {
        mController = new LiveController(this);
        mVideoView.setVideoController(mController);
    }

    private void initAdapters() {
        firstGroupAdapter = new LiveChannelGroupAdapter();
        mFirstGroupGridView.setLayoutManager(new LinearLayoutManager(this));
        mFirstGroupGridView.setAdapter(firstGroupAdapter);
        firstGroupAdapter.setOnItemClickListener((adapter, view, position) -> {
            currentFirstGroupIndex = position;
            firstGroupAdapter.setSelectedPosition(position);
            switchFirstGroup(position);
        });

        groupAdapter = new LiveChannelGroupAdapter();
        mGroupGridView.setLayoutManager(new LinearLayoutManager(this));
        mGroupGridView.setAdapter(groupAdapter);
        groupAdapter.setOnItemClickListener((adapter, view, position) -> {
            currentGroupIndex = position;
            groupAdapter.setSelectedPosition(position);
            loadChannelList(position);
        });

        channelAdapter = new LiveChannelItemAdapter();
        mChannelGridView.setLayoutManager(new LinearLayoutManager(this));
        mChannelGridView.setAdapter(channelAdapter);
        channelAdapter.setOnItemClickListener((adapter, view, position) -> {
            currentChannelIndex = position;
            channelAdapter.setSelectedPosition(position);
            playChannel(currentGroupIndex, currentChannelIndex);
            hideChannelList();
        });
        channelAdapter.setOnItemLongClickListener((adapter, view, position) -> {
            showEpgList(); return true;
        });

        epgAdapter = new LiveEpgAdapter();
        mEpgGridView.setLayoutManager(new LinearLayoutManager(this));
        mEpgGridView.setAdapter(epgAdapter);
        epgAdapter.setOnItemClickListener((adapter, view, position) -> {
            Epginfo epg = currentEpgList.get(position);
            if (epg != null) startBackLook(epg);
        });

        settingGroupAdapter = new LiveSettingGroupAdapter();
        mSettingGroupView.setLayoutManager(new LinearLayoutManager(this));
        mSettingGroupView.setAdapter(settingGroupAdapter);
        settingGroupAdapter.setOnItemClickListener((adapter, view, position) -> {
            currentSettingGroupIndex = position;
            settingGroupAdapter.setSelectedPosition(position);
            loadSettingItems(position);
        });

        settingItemAdapter = new LiveSettingItemAdapter();
        mSettingItemView.setLayoutManager(new LinearLayoutManager(this));
        mSettingItemView.setAdapter(settingItemAdapter);
        settingItemAdapter.setOnItemClickListener((adapter, view, position) -> {
            onSettingItemClick(currentSettingGroupIndex, position);
        });

        shortcutsMenuAdapter = new ShortcutsMenuAdapter();
        mShortcutsMenuGridView.setLayoutManager(new LinearLayoutManager(this));
        mShortcutsMenuGridView.setAdapter(shortcutsMenuAdapter);
        shortcutsMenuAdapter.setOnItemClickListener((adapter, view, position) -> onShortcutClick(position));

        searchChannelAdapter = new SearchChannelAdapter();
        rvSearchResult.setLayoutManager(new LinearLayoutManager(this));
        rvSearchResult.setAdapter(searchChannelAdapter);
        searchChannelAdapter.setOnItemClickListener((adapter, view, position) -> {
            jumpToChannel(searchResultList.get(position));
            hideSearch();
        });

        searchKeyboardAdapter = new SearchKeyboardAdapter();
        rvKeyboard.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvKeyboard.setAdapter(searchKeyboardAdapter);
        searchKeyboardAdapter.setOnItemClickListener((adapter, view, position) -> {
            String key = searchKeyboardAdapter.getItem(position);
            if ("DEL".equals(key)) {
                String text = etSearch.getText().toString();
                if (text.length() > 0) etSearch.setText(text.substring(0, text.length() - 1));
            } else if ("CLR".equals(key)) {
                etSearch.setText("");
            } else {
                etSearch.append(key);
            }
            doSearch(etSearch.getText().toString());
        });

        trackListAdapter = new TrackListAdapter();
        rvTrackList.setLayoutManager(new LinearLayoutManager(this));
        rvTrackList.setAdapter(trackListAdapter);
    }

    private void initData() { loadLiveSources(); loadLocalVideo(); }

    private void initListener() {
        sbProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) { currentProgress = progress; tvSeekTimeStart.setText(formatTime(progress)); }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                if (isBackLook || isTimeShift) seekToTime(currentProgress);
            }
        });
        btnSeekBack10.setOnClickListener(v -> {
            if (isBackLook || isTimeShift) {
                currentProgress = Math.max(0, currentProgress - 10000);
                sbProgress.setProgress((int) currentProgress);
                seekToTime(currentProgress);
            }
        });
        btnSeekForward10.setOnClickListener(v -> {
            if (isBackLook || isTimeShift) {
                currentProgress = Math.min(totalDuration, currentProgress + 10000);
                sbProgress.setProgress((int) currentProgress);
                seekToTime(currentProgress);
            }
        });
    }

    private void loadLiveSources() {
        // Load from ApiConfig lives
        if (!localVideoList.isEmpty()) {
            LiveChannelGroup localGroup = new LiveChannelGroup();
            localGroup.setGroupName("Local Video");
            localGroup.setGroupIndex(liveChannelGroupList.size());
            localGroup.setLiveChannels(localVideoList);
            liveChannelGroupList.add(localGroup);
        }
        initFirstGroups();
        if (!liveChannelGroupList.isEmpty()) {
            loadGroupList(0);
            playChannel(0, 0);
        }
    }

    private void initFirstGroups() {
        firstGroupList.clear();
        for (int i = 0; i < liveChannelGroupList.size(); i++) {
            LiveChannelGroup group = liveChannelGroupList.get(i);
            LiveChannelGroup first = new LiveChannelGroup();
            first.setGroupName(group.getGroupName());
            first.setGroupIndex(i);
            firstGroupList.add(first);
        }
        firstGroupAdapter.setNewData(firstGroupList);
    }

    private void switchFirstGroup(int index) {
        if (index < 0 || index >= liveChannelGroupList.size()) return;
        loadGroupList(index);
    }

    private void loadGroupList(int firstGroupIndex) {
        List<LiveChannelGroup> groups = new ArrayList<>();
        groups.add(liveChannelGroupList.get(firstGroupIndex));
        groupAdapter.setNewData(groups);
        loadChannelList(0);
    }

    private void loadChannelList(int groupIndex) {
        if (groupIndex < 0 || groupIndex >= liveChannelGroupList.size()) return;
        LiveChannelGroup group = liveChannelGroupList.get(groupIndex);
        currentChannelList = group.getLiveChannels();
        channelAdapter.setNewData(currentChannelList);
        channelAdapter.setSelectedPosition(currentChannelIndex);
    }

    private void parseM3U(String url, LiveChannelGroup group) { M3U8Subscribe.parse(url, group); }
    private void parseTXT(String url, LiveChannelGroup group) { TxtSubscribe.parse(url, group); }

    private void loadLocalVideo() {
        File videoDir = new File(getExternalFilesDir(null), LOCAL_VIDEO_DIR);
        if (!videoDir.exists()) { videoDir.mkdirs(); return; }
        scanVideoDir(videoDir, "");
    }

    private void scanVideoDir(File dir, String prefix) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                scanVideoDir(file, prefix + file.getName() + "/");
            } else {
                String name = file.getName().toLowerCase();
                if (name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi")
                        || name.endsWith(".ts") || name.endsWith(".m3u8") || name.endsWith(".flv")) {
                    LiveChannelItem item = new LiveChannelItem();
                    item.setChannelName(prefix + file.getName());
                    item.setUrlList(new ArrayList<>());
                    item.getUrlList().add(file.getAbsolutePath());
                    item.setChannelNum(localVideoList.size() + 1);
                    localVideoList.add(item);
                }
            }
        }
    }

    private void playChannel(int groupIndex, int channelIndex) {
        if (groupIndex < 0 || groupIndex >= liveChannelGroupList.size()) return;
        LiveChannelGroup group = liveChannelGroupList.get(groupIndex);
        if (channelIndex < 0 || channelIndex >= group.getLiveChannels().size()) return;

        currentGroupIndex = groupIndex;
        currentChannelIndex = channelIndex;
        currentLineIndex = 0;
        isBackLook = false;
        isTimeShift = false;

        LiveChannelItem channel = group.getLiveChannels().get(channelIndex);
        showSelectedChannel(String.valueOf(channel.getChannelNum()));

        String url = channel.getUrlList().get(currentLineIndex);
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(this, "Empty URL", Toast.LENGTH_SHORT).show();
            return;
        }

        if (url.startsWith("webview://")) {
            playWebView(url);
        } else if (url.startsWith("video://")) {
            playSniff(url);
        } else {
            playNormal(url);
        }
        updateBottomInfo(channel);
        loadEpg(channel);
        saveHistory(channel);
    }

    private void playNormal(String url) {
        loadingProgressBar.setVisibility(View.VISIBLE);
        mVideoView.setUrl(url);
        mVideoView.start();
        isPlaying = true;
    }

    private void playWebView(String url) {
        String realUrl = url.replace("webview://", "");
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra("url", realUrl);
        startActivity(intent);
    }

    private void playSniff(String url) {
        String realUrl = url.replace("video://", "");
    }

    private void showSelectedChannel(String num) {
        tvSelectedChannel.setText(num);
        tvSelectedChannel.setVisibility(View.VISIBLE);
        mHandler.removeCallbacks(mHideSelectedChannelRun);
        mHideSelectedChannelRun = () -> tvSelectedChannel.setVisibility(View.GONE);
        mHandler.postDelayed(mHideSelectedChannelRun, 2000);
    }

    private void updateBottomInfo(LiveChannelItem channel) {
        tvChannelName.setText(channel.getChannelName());
        updateCollectState(channel.getChannelName());
        updateEpgInfo(channel);
    }

    private void loadEpg(LiveChannelItem channel) {
        if (TextUtils.isEmpty(epgUrl)) epgUrl = ApiConfig.get().getEpgUrl();
        if (TextUtils.isEmpty(epgUrl)) return;
        String epgChannelName = TextUtils.isEmpty(channel.getEpg()) ? channel.getChannelName() : channel.getEpg();
        String url = buildEpgUrl(epgChannelName);
        if (TextUtils.isEmpty(url)) return;
        OkGo.<String>get(url).execute(new AbsCallback<String>() {
            @Override public void onSuccess(Response<String> response) { parseEpg(response.body(), channel); }
            @Override public String convertResponse(okhttp3.Response response) throws Exception { return response.body().string(); }
            @Override public void onError(Response<String> response) { super.onError(response); }
        });
    }

    private String buildEpgUrl(String channelName) {
        try {
            String encodedName = URLEncoder.encode(channelName, "UTF-8");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String date = sdf.format(new Date());
            return epgUrl.replace("{name}", encodedName).replace("{date}", date);
        } catch (Exception e) { return null; }
    }

    private void parseEpg(String xml, LiveChannelItem channel) {
        currentEpgList.clear();
        try {
            Pattern pattern = Pattern.compile("<programme start=\"(.*?)\" stop=\"(.*?)\".*?><title>(.*?)</title>");
            Matcher matcher = pattern.matcher(xml);
            while (matcher.find()) {
                Epginfo epg = new Epginfo();
                epg.setTitle(matcher.group(3));
                epg.setStart(matcher.group(1));
                epg.setEnd(matcher.group(2));
                currentEpgList.add(epg);
            }
        } catch (Exception e) { e.printStackTrace(); }
        updateEpgInfo(channel);
    }

    private void updateEpgInfo(LiveChannelItem channel) {
        if (currentEpgList.isEmpty()) {
            tvCurrentProgramName.setText("No EPG");
            tvCurrentProgramTime.setText("");
            tvNextProgramName.setText("");
            tvNextProgramTime.setText("");
            return;
        }
        for (int i = 0; i < currentEpgList.size(); i++) {
            Epginfo epg = currentEpgList.get(i);
            if (i == 0) {
                tvCurrentProgramName.setText(epg.getTitle());
                tvCurrentProgramTime.setText(epg.getStart() + "-" + epg.getEnd());
            }
            if (i == 1) {
                tvNextProgramName.setText(epg.getTitle());
                tvNextProgramTime.setText(epg.getStart() + "-" + epg.getEnd());
            }
        }
    }

    private void showEpgList() {
        if (currentEpgList.isEmpty()) { Toast.makeText(this, "No EPG", Toast.LENGTH_SHORT).show(); return; }
        isEpgShow = true;
        mEpgLeftLayout.setVisibility(View.VISIBLE);
        epgAdapter.setNewData(currentEpgList);
    }

    private void startBackLook(Epginfo epg) {
        isBackLook = true;
        backLookStartTime = parseEpgTime(epg.getStart());
        backLookEndTime = parseEpgTime(epg.getEnd());
        totalDuration = backLookEndTime - backLookStartTime;
        LiveChannelItem channel = getCurrentChannel();
        if (channel == null) return;
        String url = channel.getUrlList().get(currentLineIndex);
        String backUrl = buildBackLookUrl(url, backLookStartTime, backLookEndTime);
        llSeekBar.setVisibility(View.VISIBLE);
        sbProgress.setMax((int) totalDuration);
        sbProgress.setProgress(0);
        tvSeekTimeStart.setText("00:00");
        tvSeekTimeEnd.setText(formatTime(totalDuration));
        playNormal(backUrl);
    }

    private void startTimeShift() {
        isTimeShift = true;
        llSeekBar.setVisibility(View.VISIBLE);
    }

    private long parseEpgTime(String timeStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.parse(timeStr).getTime();
        } catch (Exception e) { return System.currentTimeMillis(); }
    }

    private String buildBackLookUrl(String originalUrl, long start, long end) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String startStr = sdf.format(new Date(start));
        String endStr = sdf.format(new Date(end));
        return originalUrl.replace("{playseek}", startStr + "-" + endStr);
    }

    private void seekToTime(long progress) {
        if (mVideoView != null) mVideoView.seekTo((int) progress);
    }

    private void toggleChannelList() { if (channelListIsShow) hideChannelList(); else showChannelList(); }

    private void showChannelList() {
        mPoPuLayout.setVisibility(View.VISIBLE);
        channelListIsShow = true;
        loadGroupList(currentFirstGroupIndex);
        loadChannelList(currentGroupIndex);
        channelAdapter.setSelectedPosition(currentChannelIndex);
        mHandler.removeCallbacks(mHideChannelListRun);
        mHideChannelListRun = this::hideChannelList;
        mHandler.postDelayed(mHideChannelListRun, 15000);
    }

    private void hideChannelList() {
        mPoPuLayout.setVisibility(View.GONE);
        mEpgLeftLayout.setVisibility(View.GONE);
        isEpgShow = false;
        channelListIsShow = false;
    }

    private void showBottomInfo() {
        tvBottomLayout.setVisibility(View.VISIBLE);
        tvTimeGroup.setVisibility(View.VISIBLE);
        tvNetSpeed.setVisibility(View.VISIBLE);
        bottomInfoIsShow = true;
        mHandler.removeCallbacks(mHideBottomInfoRun);
        mHideBottomInfoRun = this::hideBottomInfo;
        mHandler.postDelayed(mHideBottomInfoRun, 5000);
    }

    private void hideBottomInfo() {
        tvBottomLayout.setVisibility(View.GONE);
        tvTimeGroup.setVisibility(View.GONE);
        tvNetSpeed.setVisibility(View.GONE);
        bottomInfoIsShow = false;
    }

    private void toggleSearch() { if (isSearchShow) hideSearch(); else showSearch(); }

    private void showSearch() {
        isSearchShow = true;
        mSearchLayout.setVisibility(View.VISIBLE);
        etSearch.setText("");
        searchResultList.clear();
        searchChannelAdapter.setNewData(searchResultList);
        List<String> keys = new ArrayList<>();
        for (char c = 'A'; c <= 'Z'; c++) keys.add(String.valueOf(c));
        for (char c = '0'; c <= '9'; c++) keys.add(String.valueOf(c));
        keys.add("DEL"); keys.add("CLR");
        searchKeyboardAdapter.setNewData(keys);
    }

    private void hideSearch() { isSearchShow = false; mSearchLayout.setVisibility(View.GONE); }

    private void doSearch(String keyword) {
        searchResultList.clear();
        if (TextUtils.isEmpty(keyword)) { searchChannelAdapter.setNewData(searchResultList); return; }
        for (LiveChannelGroup group : liveChannelGroupList) {
            for (LiveChannelItem item : group.getLiveChannels()) {
                if (item.getChannelName().toLowerCase().contains(keyword.toLowerCase())) {
                    searchResultList.add(item);
                }
            }
        }
        searchChannelAdapter.setNewData(searchResultList);
    }

    private void jumpToChannel(LiveChannelItem item) {
        for (int i = 0; i < liveChannelGroupList.size(); i++) {
            List<LiveChannelItem> channels = liveChannelGroupList.get(i).getLiveChannels();
            for (int j = 0; j < channels.size(); j++) {
                if (channels.get(j) == item) {
                    currentFirstGroupIndex = i; currentGroupIndex = i; currentChannelIndex = j;
                    playChannel(i, j); return;
                }
            }
        }
    }

    private void toggleSetting() { if (isSettingShow) hideSetting(); else showSetting(); }
    private void showSetting() { isSettingShow = true; mSettingLayout.setVisibility(View.VISIBLE); loadSettingGroups(); }
    private void hideSetting() { isSettingShow = false; mSettingLayout.setVisibility(View.GONE); }

    private void initSettingGroups() {
        settingGroupList.clear();
        LiveSettingGroup decodeGroup = new LiveSettingGroup();
        decodeGroup.setGroupName("Decode");
        List<LiveSettingItem> decodeItems = new ArrayList<>();
        decodeItems.add(createSettingItem("System", 0));
        decodeItems.add(createSettingItem("IJK", 1));
        decodeItems.add(createSettingItem("EXO", 2));
        decodeGroup.setLiveSettingItems(decodeItems);
        settingGroupList.add(decodeGroup);

        LiveSettingGroup ratioGroup = new LiveSettingGroup();
        ratioGroup.setGroupName("Aspect Ratio");
        List<LiveSettingItem> ratioItems = new ArrayList<>();
        ratioItems.add(createSettingItem("Default", 0));
        ratioItems.add(createSettingItem("16:9", 1));
        ratioItems.add(createSettingItem("4:3", 2));
        ratioItems.add(createSettingItem("Fill", 3));
        ratioItems.add(createSettingItem("Original", 4));
        ratioGroup.setLiveSettingItems(ratioItems);
        settingGroupList.add(ratioGroup);

        LiveSettingGroup lineGroup = new LiveSettingGroup();
        lineGroup.setGroupName("Line");
        settingGroupList.add(lineGroup);

        LiveSettingGroup prefGroup = new LiveSettingGroup();
        prefGroup.setGroupName("Preference");
        List<LiveSettingItem> prefItems = new ArrayList<>();
        prefItems.add(createSettingItem("Auto Line: " + (isAutoChangeLine ? "ON" : "OFF"), 0));
        prefItems.add(createSettingItem("Auto Source: " + (isAutoChangeSource ? "ON" : "OFF"), 1));
        prefItems.add(createSettingItem("Show Logo: ON", 2));
        prefItems.add(createSettingItem("Show EPG: ON", 3));
        prefGroup.setLiveSettingItems(prefItems);
        settingGroupList.add(prefGroup);

        LiveSettingGroup localGroup = new LiveSettingGroup();
        localGroup.setGroupName("Local Video");
        List<LiveSettingItem> localItems = new ArrayList<>();
        localItems.add(createSettingItem("Scan", 0));
        localItems.add(createSettingItem("Clear", 1));
        localGroup.setLiveSettingItems(localItems);
        settingGroupList.add(localGroup);

        LiveSettingGroup otherGroup = new LiveSettingGroup();
        otherGroup.setGroupName("Other");
        List<LiveSettingItem> otherItems = new ArrayList<>();
        otherItems.add(createSettingItem("Theme", 0));
        otherItems.add(createSettingItem("Backup", 1));
        otherItems.add(createSettingItem("Remote", 2));
        otherItems.add(createSettingItem("About", 3));
        otherGroup.setLiveSettingItems(otherItems);
        settingGroupList.add(otherGroup);
    }

    private LiveSettingItem createSettingItem(String name, int value) {
        LiveSettingItem item = new LiveSettingItem();
        item.setItemName(name);
        item.setItemValue(value);
        return item;
    }

    private void loadSettingGroups() {
        LiveChannelItem channel = getCurrentChannel();
        if (channel != null && channel.getUrlList() != null) {
            List<LiveSettingItem> lineItems = new ArrayList<>();
            for (int i = 0; i < channel.getUrlList().size(); i++) {
                lineItems.add(createSettingItem("Line " + (i + 1), i));
            }
            settingGroupList.get(2).setLiveSettingItems(lineItems);
        }
        settingGroupAdapter.setNewData(settingGroupList);
        settingGroupAdapter.setSelectedPosition(currentSettingGroupIndex);
        loadSettingItems(currentSettingGroupIndex);
    }

    private void loadSettingItems(int groupIndex) {
        if (groupIndex < 0 || groupIndex >= settingGroupList.size()) return;
        settingItemAdapter.setNewData(settingGroupList.get(groupIndex).getLiveSettingItems());
        settingItemAdapter.setSelectedPosition(currentSettingItemIndex);
    }

    private void onSettingItemClick(int groupIndex, int itemIndex) {
        switch (groupIndex) {
            case 0: Hawk.put(HawkConfig.LIVE_PLAYER_TYPE, itemIndex); Toast.makeText(this, "Decode switched", Toast.LENGTH_SHORT).show(); break;
            case 1: setVideoScale(itemIndex); break;
            case 2: currentLineIndex = itemIndex; playChannel(currentGroupIndex, currentChannelIndex); break;
            case 3: onPrefSettingClick(itemIndex); break;
            case 4: onLocalVideoClick(itemIndex); break;
            case 5: onOtherSettingClick(itemIndex); break;
        }
        hideSetting();
    }

    private void setVideoScale(int scaleType) {
        switch (scaleType) {
            case 0: mVideoView.setScreenScaleType(VideoView.SCREEN_SCALE_DEFAULT); break;
            case 1: mVideoView.setScreenScaleType(VideoView.SCREEN_SCALE_16_9); break;
            case 2: mVideoView.setScreenScaleType(VideoView.SCREEN_SCALE_4_3); break;
            case 3: mVideoView.setScreenScaleType(VideoView.SCREEN_SCALE_MATCH_PARENT); break;
            case 4: mVideoView.setScreenScaleType(VideoView.SCREEN_SCALE_ORIGINAL); break;
        }
    }

    private void onPrefSettingClick(int index) {
        switch (index) {
            case 0: isAutoChangeLine = !isAutoChangeLine; Toast.makeText(this, "Auto Line: " + (isAutoChangeLine ? "ON" : "OFF"), Toast.LENGTH_SHORT).show(); break;
            case 1: isAutoChangeSource = !isAutoChangeSource; Toast.makeText(this, "Auto Source: " + (isAutoChangeSource ? "ON" : "OFF"), Toast.LENGTH_SHORT).show(); break;
        }
    }

    private void onLocalVideoClick(int index) {
        switch (index) {
            case 0: loadLocalVideo(); Toast.makeText(this, "Scanned", Toast.LENGTH_SHORT).show(); break;
            case 1: localVideoList.clear(); Toast.makeText(this, "Cleared", Toast.LENGTH_SHORT).show(); break;
        }
    }

    private void onOtherSettingClick(int index) {
        switch (index) {
            case 0: toggleTheme(); break;
            case 1: showBackupDialog(); break;
            case 2: startRemoteInput(); break;
            case 3: showAboutDialog(); break;
        }
    }

    private void initShortcuts() {
        shortcutsList.clear();
        shortcutsList.add(new ShortcutsBean("Collect", R.drawable.ic_collect));
        shortcutsList.add(new ShortcutsBean("Search", R.drawable.ic_search));
        shortcutsList.add(new ShortcutsBean("Settings", R.drawable.ic_setting));
        shortcutsList.add(new ShortcutsBean("Info", R.drawable.ic_info));
        shortcutsList.add(new ShortcutsBean("Track", R.drawable.ic_track));
        shortcutsList.add(new ShortcutsBean("TimeShift", R.drawable.ic_timeshift));
        shortcutsMenuAdapter.setNewData(shortcutsList);
    }

    private void onShortcutClick(int position) {
        switch (position) {
            case 0: toggleCollect(); break;
            case 1: toggleSearch(); break;
            case 2: toggleSetting(); break;
            case 3: togglePlayMessage(); break;
            case 4: showTrackDialog(); break;
            case 5: startTimeShift(); break;
        }
    }

    private void loadCollect() { collectList = Hawk.get(COLLECT_KEY, new ArrayList<>()); }
    private void saveCollect() { Hawk.put(COLLECT_KEY, collectList); }

    private void toggleCollect() {
        LiveChannelItem channel = getCurrentChannel();
        if (channel == null) return;
        String name = channel.getChannelName();
        if (collectList.contains(name)) {
            collectList.remove(name);
            Toast.makeText(this, "Removed", Toast.LENGTH_SHORT).show();
        } else {
            collectList.add(name);
            Toast.makeText(this, "Added", Toast.LENGTH_SHORT).show();
        }
        saveCollect();
        updateCollectState(name);
    }

    private void updateCollectState(String channelName) {
        if (collectList.contains(channelName)) {
            tvChannelCollect.setBackgroundResource(R.drawable.ic_collect_selected);
        } else {
            tvChannelCollect.setBackgroundResource(R.drawable.ic_collect_normal);
        }
    }

    private void togglePlayMessage() {
        if (isPlayMessageShow) { mPlayMessageLayout.setVisibility(View.GONE); isPlayMessageShow = false; }
        else showPlayMessage();
    }

    private void showPlayMessage() {
        isPlayMessageShow = true;
        mPlayMessageLayout.setVisibility(View.VISIBLE);
        LiveChannelItem channel = getCurrentChannel();
        if (channel != null) tvVideoUrl.setText("URL: " + channel.getUrlList().get(currentLineIndex));
        tvVideoFormat.setText("Format: ");
        tvVideoCodec.setText("Codec: ");
        tvVideoResolution.setText("Resolution: ");
        tvAudioCodec.setText("Audio: ");
        tvPlayerConfig.setText("Config: ");
        mHandler.postDelayed(() -> { mPlayMessageLayout.setVisibility(View.GONE); isPlayMessageShow = false; }, 5000);
    }

    private void showTrackDialog() {
        isTrackShow = true;
        mTrackLayout.setVisibility(View.VISIBLE);
        List<String> tracks = new ArrayList<>();
        tracks.add("Default");
        trackListAdapter.setNewData(tracks);
    }

    private void toggleTheme() {
        if (isThemeShow) { mThemeLayout.setVisibility(View.GONE); isThemeShow = false; }
        else { isThemeShow = true; mThemeLayout.setVisibility(View.VISIBLE); }
    }

    private void showBackupDialog() {}
    private void startRemoteInput() { Toast.makeText(this, "Remote Input", Toast.LENGTH_SHORT).show(); }
    private void showAboutDialog() { Toast.makeText(this, "TVBoxOS + Ku9", Toast.LENGTH_SHORT).show(); }

    private void switchChannel(boolean isNext) {
        LiveChannelGroup group = liveChannelGroupList.get(currentGroupIndex);
        int size = group.getLiveChannels().size();
        if (isNext) currentChannelIndex = (currentChannelIndex + 1) % size;
        else currentChannelIndex = (currentChannelIndex - 1 + size) % size;
        playChannel(currentGroupIndex, currentChannelIndex);
        showBottomInfo();
    }

    private void switchLine(boolean isNext) {
        LiveChannelItem channel = getCurrentChannel();
        if (channel == null) return;
        List<String> urls = channel.getUrlList();
        if (urls == null || urls.size() <= 1) return;
        if (isNext) currentLineIndex = (currentLineIndex + 1) % urls.size();
        else currentLineIndex = (currentLineIndex - 1 + urls.size()) % urls.size();
        playChannel(currentGroupIndex, currentChannelIndex);
        Toast.makeText(this, "Line " + (currentLineIndex + 1) + "/" + urls.size(), Toast.LENGTH_SHORT).show();
    }

    private void switchGroup(boolean isNext) {
        int size = liveChannelGroupList.size();
        if (isNext) currentGroupIndex = (currentGroupIndex + 1) % size;
        else currentGroupIndex = (currentGroupIndex - 1 + size) % size;
        currentChannelIndex = 0;
        playChannel(currentGroupIndex, currentChannelIndex);
        showBottomInfo();
    }

    private void inputChannelNumber(int num) {
        selectedChannelNum = selectedChannelNum * 10 + num;
        tvSelectedChannel.setText(String.valueOf(selectedChannelNum));
        tvSelectedChannel.setVisibility(View.VISIBLE);
        mHandler.removeCallbacks(mHideSelectedChannelRun);
        mHideSelectedChannelRun = () -> {
            tvSelectedChannel.setVisibility(View.GONE);
            jumpToChannelNumber(selectedChannelNum);
            selectedChannelNum = 0;
        };
        mHandler.postDelayed(mHideSelectedChannelRun, 2000);
    }

    private void jumpToChannelNumber(int num) {
        for (int i = 0; i < liveChannelGroupList.size(); i++) {
            List<LiveChannelItem> channels = liveChannelGroupList.get(i).getLiveChannels();
            for (int j = 0; j < channels.size(); j++) {
                if (channels.get(j).getChannelNum() == num) {
                    currentGroupIndex = i; currentChannelIndex = j;
                    playChannel(i, j); return;
                }
            }
        }
        Toast.makeText(this, "Channel not found", Toast.LENGTH_SHORT).show();
    }

    private void startTimeUpdate() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        timeUpdateRun = new Runnable() {
            @Override public void run() {
                tvTime.setText(sdf.format(new Date()));
                mHandler.postDelayed(this, 1000);
            }
        };
        mHandler.post(timeUpdateRun);
    }

    private void startNetSpeedTimer() {
        netSpeedTimer = new Timer();
        netSpeedTimer.schedule(new TimerTask() {
            @Override public void run() {
                runOnUiThread(() -> {
                    // Net speed update
                });
            }
        }, 0, 1000);
    }

    private String formatTime(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes % 60, seconds % 60);
    }

    private LiveChannelItem getCurrentChannel() {
        if (liveChannelGroupList.isEmpty()) return null;
        if (currentGroupIndex >= liveChannelGroupList.size()) return null;
        LiveChannelGroup group = liveChannelGroupList.get(currentGroupIndex);
        if (currentChannelIndex >= group.getLiveChannels().size()) return null;
        return group.getLiveChannels().get(currentChannelIndex);
    }

    private void saveHistory(LiveChannelItem channel) {}

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (isSettingShow || isSearchShow) return false;
                    if (!channelListIsShow) switchChannel(false);
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (isSettingShow || isSearchShow) return false;
                    if (!channelListIsShow) switchChannel(true);
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (isSettingShow || isSearchShow) return false;
                    if (!channelListIsShow) switchLine(false);
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (isSettingShow || isSearchShow) return false;
                    if (!channelListIsShow) switchLine(true);
                    return true;
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    if (isSettingShow || isSearchShow) return false;
                    toggleChannelList();
                    return true;
                case KeyEvent.KEYCODE_MENU:
                    toggleSetting();
                    return true;
                case KeyEvent.KEYCODE_INFO:
                    togglePlayMessage();
                    return true;
                case KeyEvent.KEYCODE_0: inputChannelNumber(0); return true;
                case KeyEvent.KEYCODE_1: inputChannelNumber(1); return true;
                case KeyEvent.KEYCODE_2: inputChannelNumber(2); return true;
                case KeyEvent.KEYCODE_3: inputChannelNumber(3); return true;
                case KeyEvent.KEYCODE_4: inputChannelNumber(4); return true;
                case KeyEvent.KEYCODE_5: inputChannelNumber(5); return true;
                case KeyEvent.KEYCODE_6: inputChannelNumber(6); return true;
                case KeyEvent.KEYCODE_7: inputChannelNumber(7); return true;
                case KeyEvent.KEYCODE_8: inputChannelNumber(8); return true;
                case KeyEvent.KEYCODE_9: inputChannelNumber(9); return true;
                case KeyEvent.KEYCODE_BACK:
                    if (channelListIsShow) { hideChannelList(); return true; }
                    if (isSettingShow) { hideSetting(); return true; }
                    if (isSearchShow) { hideSearch(); return true; }
                    if (isPlayMessageShow) { mPlayMessageLayout.setVisibility(View.GONE); isPlayMessageShow = false; return true; }
                    if (isTrackShow) { mTrackLayout.setVisibility(View.GONE); isTrackShow = false; return true; }
                    if (isThemeShow) { mThemeLayout.setVisibility(View.GONE); isThemeShow = false; return true; }
                    if (isEpgShow) { mEpgLeftLayout.setVisibility(View.GONE); isEpgShow = false; return true; }
                    if (llSeekBar.getVisibility() == View.VISIBLE) { llSeekBar.setVisibility(View.GONE); return true; }
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private float startX, startY;
    private static final int SWIPE_THRESHOLD = 100;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX(); startY = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                float endX = event.getX(), endY = event.getY();
                float dx = endX - startX, dy = endY - startY;
                float screenWidth = getResources().getDisplayMetrics().widthPixels;
                float screenHeight = getResources().getDisplayMetrics().heightPixels;

                if (Math.abs(dx) < SWIPE_THRESHOLD && Math.abs(dy) < SWIPE_THRESHOLD) {
                    if (endX > screenWidth * 0.3 && endX < screenWidth * 0.7
                            && endY > screenHeight * 0.3 && endY < screenHeight * 0.7) {
                        showBottomInfo();
                    }
                    return true;
                }
                if (Math.abs(dx) > Math.abs(dy)) {
                    if (dx > 0) switchLine(true); else switchLine(false);
                } else {
                    if (startX < screenWidth / 2) {
                        // Left half: volume
                    } else {
                        if (dy > 0) switchChannel(true); else switchChannel(false);
                    }
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override protected void onPause() { super.onPause(); if (mVideoView != null) mVideoView.pause(); }
    @Override protected void onResume() { super.onResume(); if (mVideoView != null) mVideoView.resume(); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHandler != null) mHandler.removeCallbacksAndMessages(null);
        if (netSpeedTimer != null) netSpeedTimer.cancel();
        if (mVideoView != null) mVideoView.release();
    }
}
