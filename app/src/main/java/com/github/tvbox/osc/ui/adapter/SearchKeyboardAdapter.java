package com.github.tvbox.osc.ui.adapter;

import android.widget.TextView;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import java.util.ArrayList;

public class SearchKeyboardAdapter extends BaseQuickAdapter<String, BaseViewHolder> {
    public SearchKeyboardAdapter() {
        super(R.layout.item_search_keyboard, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, String item) {
        helper.setText(R.id.tvKey, item);
    }
}
