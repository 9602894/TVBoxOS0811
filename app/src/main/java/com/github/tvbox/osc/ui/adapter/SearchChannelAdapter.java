package com.github.tvbox.osc.ui.adapter;

import android.widget.TextView;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.LiveChannelItem;
import java.util.ArrayList;

public class SearchChannelAdapter extends BaseQuickAdapter<LiveChannelItem, BaseViewHolder> {
    public SearchChannelAdapter() {
        super(R.layout.item_search_channel, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, LiveChannelItem item) {
        helper.setText(R.id.tvSearchChannelName, item.getChannelName());
        helper.setText(R.id.tvSearchChannelNum, String.valueOf(item.getChannelNum()));
    }
}
